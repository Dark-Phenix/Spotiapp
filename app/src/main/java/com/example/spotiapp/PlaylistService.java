package com.example.spotiapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import retrofit.client.Response;


/**
 * Covers all methods related to work with playlists
 */
public class PlaylistService {

    public static final String PLAYLIST_DATABASE = "playlist_database.json";


    private final SpotifyService spotify;
    private final String userID;
    private final Context context;
    private final SharedPreferences playlistPrefs;

    private boolean  _imagesUpdated;


    //ID, MyPlaylist Referenece
    private Map<String, MyPlaylist> _myPlaylistsMap;

    public PlaylistService(SpotifyService spotify, String userid, Context context){
        this.spotify = spotify;
        this.userID = userid;
        this.context = context;
        playlistPrefs = context.getSharedPreferences("PLAYLISTS", 0);

        _imagesUpdated = false;

        // Datei anlegen, wenn nicht existiert
        if (!FileMethods.fileExists(context, PLAYLIST_DATABASE)) {
            FileMethods.inFile(context, PLAYLIST_DATABASE, new ArrayList<MyPlaylist>());
        }

        loadPlaylists();
    }


    private synchronized void loadPlaylists()
    {
        List<MyPlaylist> myPlaylistsList;
        myPlaylistsList = FileMethods.listFromJson(context, PLAYLIST_DATABASE, MyPlaylist.class);
        if (myPlaylistsList == null)
        {
            myPlaylistsList = new ArrayList<>();
        }
        _myPlaylistsMap = new HashMap<>();
        for (MyPlaylist elem : myPlaylistsList)
        {
            //if (!(elem.getName().equals("app-uk")))
            //{
                _myPlaylistsMap.put(elem.getId(), elem);
            //}
        }
    }

    /**
     * Add songs to the playlist, only if they aren't in the playlist yet
     * @param playlistID playlist ID
     * @param songList List of myTracks to add
     */
    public synchronized void addSongs(String playlistID, List<MyTrack> songList) {
        Set<PlaylistTrack> songids = new HashSet<>();

        int le = songList.size();

        spotify.getPlaylistTracks(userID, playlistID, Map.of("fields", "total"), new SpotifyCallback<>() {

            @Override
            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                final int total = playlistTrackPager.total;

                // remove the tracks from the list, if they occur in songList and ID
                for (int i = 0; i < total; i += 50) {
                    spotify.getPlaylistTracks(userID, playlistID, Map.of("market", "DE", "limit", 50, "offset", i) , new SpotifyCallback<>() {
                        @Override
                        public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                            songids.addAll(playlistTrackPager.items);
                            if (songids.size()==total) { // the last request
                                List<MyTrack> valuesToRemove = new ArrayList<>();
                                for (MyTrack mt : songList) {
                                    String songid = mt.id;
                                    for (PlaylistTrack pt : songids) {
                                        if (songid.equals(pt.track.id))
                                        {
                                            valuesToRemove.add(mt);
                                        }
                                    }
                                }
                                Log.d("PLAYLIST", String.valueOf(songList.size()));
                                songList.removeAll(valuesToRemove);
                                Log.d("PLAYLIST", "REMOVED DOUBLES");
                                Log.d("PLAYLIST", String.valueOf(songList.size()));
                                realAddSongs(playlistID, songList);
                            }
                        }

                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.e("PLAYLIST", spotifyError.toString());
                        }
                    });
                }
                // if playlist is empty just add songs
                if (total == 0)
                {
                    realAddSongs(playlistID, songList);
                }
            }

            @Override
            public void failure(SpotifyError spotifyError) {
                Log.e("REQUEST", spotifyError.toString());
            }
        });
    }

    //private method to add songs, from addSongs
    private synchronized void realAddSongs(String playlistID, List<MyTrack> songList)
    {
        List<String> finishUri = uriBuilder(songList, 50);
        for (String uri : finishUri) {
            spotify.addTracksToPlaylist(userID, playlistID, Map.of("position", 0, "uris", uri), Map.of(), new SpotifyCallback<>() {

                @Override
                public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                    Objects.requireNonNull(_myPlaylistsMap.get(playlistID)).setLastUpdated(LocalDateTime.now());
                    updateDatabase();
                    Log.d("PLAYLIST", "added songs");
                }

                @Override
                public void failure(SpotifyError spotifyError) {
                    Log.e("PLAYLIST", spotifyError.toString());
                }
            });
        }
    }

    //separates the MyTrack List in a List of String with the length of MAX
    // needed for the requests
    private synchronized List<String> uriBuilder(List<MyTrack> songs, int max){
        List<String> myURIs = new ArrayList<>();


        int len = songs.size();
        for (int j = 0; j<len; j+=max) {// steps in size of 50, because its the maximum of 1 request
            int end = j + max;    //end of the inner for loop
            if (end > len) {
                end = len;     //not array out of bounds
            }
            StringBuilder all = new StringBuilder(); //String Builder, because
            // the Ids need to be in a comma separated String

            all.append("spotify:track:").append(songs.get(j).id);     //before first element no comma
            for (int i = j + 1; i < end; i++) {
                all.append(",").append("spotify:track:").append(songs.get(i).id); //else comma separated
            }
            myURIs.add(all.toString());
        }
        Log.d("URI_LIST", "build finish");
        return myURIs;
    }

    /**
     * Add URL from Playlist Cover to the Map of Covers
     * @param playlistID die SPotify ID zur Playlist
     */
    public synchronized void updateImage(String playlistID)
    {
        Log.d("PLAYLIST_IMG", "START");
        spotify.getPlaylist(userID, playlistID, new SpotifyCallback<>() {
            @Override
            public void success(Playlist playlist, Response response) {
                if (playlist.images != null && !playlist.images.isEmpty()) {
                    String imageUrl = playlist.images.get(0).url; // Die URL des Playlist-Bildes
                    Log.d("PLAYLIST_IMG", imageUrl);
                    //_images.put(playlistID, imageUrl);
                    Objects.requireNonNull(_myPlaylistsMap.get(playlistID)).setImage(imageUrl);
                    updateDatabase();
                    _imagesUpdated = true;
                }
                else
                {
                    Log.e("PLAYLIST_IMG", "Playlist has no Image");
                }
            }

            @Override
            public void failure(SpotifyError error) {
                Log.e("PLAYLIST_IMG", error.toString());
            }
        });
        Log.d("PLAYLIST_IMG", "ENDE");
    }


    public synchronized boolean getUpdated()
    {
        return _imagesUpdated;
    }


    public synchronized void createPlaylist(String name, String description, Set<String> genres, List<String> artists)
    {
        spotify.createPlaylist(userID, Map.of("name", name, "description", description, "public", true), new SpotifyCallback<Playlist>() {

            @Override
            public void success(Playlist playlist, Response response) {
                MyPlaylist pla = new MyPlaylist(name,  playlist.id, description, null, genres, artists, null);
                _myPlaylistsMap.put(playlist.id, pla);
                updateDatabase();
                Log.d("CREATED_PLAYLIST", playlist.id);
            }
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.e("CREATE_PLAYLIST", spotifyError.toString());
            }
        });
    }

    public synchronized MyPlaylist getPlaylist(String playlistID)
    {
        return _myPlaylistsMap.get(playlistID);
    }

    public synchronized Map<String, MyPlaylist> getMyPlaylistsMap() {
        return _myPlaylistsMap;
    }

    //call always after change of the playlists
    private synchronized void updateDatabase()
    {
        //TODO Threadsafe
        FileMethods.inFile(context, PLAYLIST_DATABASE, new ArrayList<>(_myPlaylistsMap.values()));
    }
}
