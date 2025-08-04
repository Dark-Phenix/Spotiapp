package com.example.spotiapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import com.android.volley.toolbox.Volley;
import com.example.spotiapp.ui.GenreAdapter;
import com.example.spotiapp.ui.PlaylistView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.android.volley.RequestQueue;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.squareup.picasso.Picasso;


import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences.Editor editor;
    private SharedPreferences msharedPreferences;

    private RequestQueue queue;

    private static final String CLIENT_ID = "0d5b4b74b53f4933a2647a4d3fe182de";
    private static final String REDIRECT_URI = "spotiapp://authenticationresponse";
    private static final String[] SCOPES =
            {"user-library-read", "playlist-modify-public", "playlist-read-collaborative", "user-read-private"};
    private static final int REQUEST_CODE = 1337;
    private SpotifyAppRemote mSpotifyAppRemote;
    private String accessToken;

    EditText mEditGenre;;
    ScrollView mScrollView;
    LinearLayout mLayout;

    FrameLayout loadingOverlay;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditGenre = findViewById(R.id.genreText);
        mScrollView = findViewById(R.id.scroll_view);
        mLayout = findViewById(R.id.vertical_layout);
        Log.d("MainActivity", "mLayout after findViewById: " + mLayout);
        loadingOverlay = findViewById(R.id.loading_overlay);  // <== Overlay initialisieren

        msharedPreferences = this.getSharedPreferences("SPOTIFY", 0);
        queue = Volley.newRequestQueue(this);

        showLoading(true);  // <== Loading anzeigen
        spotifyAuthentication();  // <== Auth starten
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }




    @Override
    protected void onStart() {

        //UM mit der App zu interagieren. Erstmal nur die WEB Api benutzen
        super.onStart();


        /*ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();
        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");
                    }

                    public void onFailure(Throwable throwable) {
                        Log.e("MyActivity", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    private void spotifyAuthentication() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(SCOPES);
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    // "Main-method after the authorization"
    private void connected() {
        Log.d("CONNECTED", "start main");
        SpotifyApi api = new SpotifyApi();
        api.setAccessToken(accessToken);
        SpotifyService spotify = api.getService();
        String userid = msharedPreferences.getString("userid", "No User");


        Favourite fav = new Favourite(spotify, this, msharedPreferences);

        // Listener setzen, bevor `create_fav_database()` aufgerufen wird!
        fav.setFavDatabaseListener(() -> {
            Log.d("DATABASE", "Callback received");

            Set<MyTrack> mtl = fav.createMyTrackSet();
            Log.d("DATABASE READY", "My track list size: " + mtl.size());
            PlaylistService playlistService = new PlaylistService(spotify, userid, this);

            Set<String> genreList2 = new HashSet<>();
            //genreList2.add("uk drill");
            //playlistService.createPlaylist("app-uk-drill", "automate from Android App", genreList2, null);

            loadAllPlaylists(mtl, playlistService);
            showLoading(false);
        });

        fav.ensureFavDatabaseAsync()
                .thenRun(() -> {
                    // Hier kommt der Code, der ausgeführt werden soll, wenn die Favoriten-Datenbank bereit ist.
                    // Zum Beispiel UI-Update oder Listener informieren
                })
                .exceptionally(ex -> {
                    // Fehlerbehandlung, falls etwas schiefgeht
                    ex.printStackTrace();
                    return null;
                });





//        Set<String> genreList = new HashSet<>();
//        genreList.add("rap");
//        playlistService.createPlaylist("app-rap", "automate from Android App", genreList, null);
//
//        Set<String> genreList2 = new HashSet<>();
//        genreList2.add("german hip hop");
//        playlistService.createPlaylist("app-german", "automate from Android App", genreList2, null);

        //findViewById(R.id.updateButton).setOnClickListener(view -> fav.create_fav_database()); //IN JSON SPEICHERN TODO update was anderes als erstmalig erstellen?
    }

    private void createAppPlaylists(Set<MyTrack> mtl, PlaylistService playlistService)
    {
        for (MyPlaylist my : playlistService.getMyPlaylistsMap().values()) {
            createReadyPlaylist(mtl, playlistService, my.getId());
        }
    }


    private void createReadyPlaylist(Set<MyTrack> mtl, PlaylistService playlistService, String playlistID)
    {
        Log.d("MainActivity", "Create Ready Playlist; mtl size:" + mtl.size());
        MyPlaylist playlist = playlistService.getPlaylist(playlistID);
        Set<String> genres = playlist.getGenres();
        String id = playlist.getId();
        Set<MyTrack> genreSet = Favourite.readGenres(mtl, genres);
        playlistService.addSongs(id, new ArrayList<>(genreSet));
        playlistService.updateImage(playlistID);
    }


    //load all playlists, should be run at start
    private void loadAllPlaylists(Set<MyTrack> mtl, PlaylistService playlistService)
    {
        Log.d("Test", "Anzahl an Playlists: " + playlistService.getMyPlaylistsMap().size());
        if (mLayout == null) {
            Log.e("MainActivity", "mLayout is null in loadAllPlaylists!");
            return;
        }
        mLayout.removeAllViewsInLayout();
        for (MyPlaylist my : playlistService.getMyPlaylistsMap().values()) {
            PlaylistView myView = new PlaylistView(this);
            myView.setPlaylist(R.mipmap.playlist_icon, my.getName());

            ImageView imgV = myView.getPlaylistImage();

            myView.getRefreshView().setOnClickListener(view -> refresh(my, myView, mtl, playlistService));

            String imgPath = playlistService.getPlaylist(my.getId()).getImage();
            if (imgPath == null)
            {
                Picasso.get().load(R.mipmap.playlist_icon).into(imgV);
                Log.d("PICASSO", "Loaded standard Image");
            }
            else
            {
                Picasso.get().load(imgPath).into(imgV);
                Log.d("PICASSO", imgPath);
            }
            mLayout.addView(myView);
        }
    }

    private void refresh(MyPlaylist pla, PlaylistView plaV, Set<MyTrack> mtl, PlaylistService playlistService)
    {
        //printing
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String dateTimeString = now.format(formatter);
        plaV.getDescriptionView().setText(dateTimeString);

        //real update
        createReadyPlaylist(mtl, playlistService, pla.getId());
    }


    private void getSong(SpotifyService spotify) {
        spotify.getTrack("11dFghVXANMlKmJXsNCbNl", new Callback<Track>() {
            @Override
            public void success(Track track, Response response) {
                Log.d("REQUEST", track.name);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("REQUEST", error.toString());
            }
        });
    }


    //Standard Methode
    private void connectedStandard() {
        // Play a playlist
        mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");

        // Subscribe to PlayerState
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    //final Track track = playerState.track;
                    ////}
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            Log.d("SPOTIFY", "Auth type: " + response.getType());
            Log.d("SPOTIFY", "Auth error: " + response.getError());
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    editor = getSharedPreferences("SPOTIFY", 0).edit();
                    editor.putString("token", response.getAccessToken());
                    Log.d("STARTING", "GOT AUTH TOKEN");
                    editor.apply();

                    accessToken = response.getAccessToken();

                    if (!msharedPreferences.contains("userid")) {
                        waitForUserInfo(); // -> ruft connected() nach dem Abrufen auf
                    } else {
                        connected();
                    }
                    break;

                // Auth flow returned an error
                case ERROR:
                    Log.e("STARTING", response.getError());
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    Log.d("DEFAULT", "OTHER CASE");
                    // Handle other cases
            }
        }
    }

    private void waitForUserInfo() {
        UserService userService = new UserService(queue, msharedPreferences);
        userService.get(() -> {
            MyUser user = userService.getUser();
            editor = getSharedPreferences("SPOTIFY", 0).edit();
            editor.putString("userid", user.id);
            Log.d("STARTING", "GOT USER INFORMATION");
            // We use commit instead of apply because we need the information stored immediately
            editor.commit();

            // Got the User
            connected();
        });
    }

}