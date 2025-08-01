package com.example.spotiapp;



import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Artists;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.SavedTrack;

import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;



/**
 * Behandelt alle Methoden aus deer py Datei, die auf den Favourite Songs basieren
 */
public class Favourite {
    //Konstanten
    public static final String ARTIST_DATABASE = "artist_fav_database.json";
    public static final String SONG_DATABASE = "favourite_database.json";

    private final SpotifyService spotify;

    private final Context context;

    private SharedPreferences msharedPreferences;

    public Favourite(SpotifyService spoService, Context con, SharedPreferences sharedPreferences){
        spotify = spoService;
        context = con;
        msharedPreferences = sharedPreferences;
    }

    /**
     * Reads the genre out of an myTrack List
     * @param songList Die komplette Songliste, die durchsucht werden soll
     * @param genre The genre; eg. "rap", "german hip hop", "dance pop", "uk hip hop"
     * @return Tracklist mit passenden Songs zu der Genre
     */
    public static Set<MyTrack> read_genre(Set<MyTrack> songList, String genre) {
        Set<MyTrack> genreSongs = new HashSet<>();
        for (MyTrack song : songList){
            if (song.genres.contains(genre)){
                genreSongs.add(song);
            }
        }
        return genreSongs;
    }

    public static Set<MyTrack> readGenres(Set<MyTrack> songList, Set<String> genres) {
        Set<MyTrack> allSongs = new HashSet<>();
        for (String genre : genres)
        {
            allSongs.addAll(read_genre(songList, genre));
        }
        return allSongs;
    }

    /** Creates a list with MyTracks with all important information
     *
     * @return Die Tracklist
     */
    public Set<MyTrack> createMyTrackSet()
    {
        Set<MyTrack> songs = new HashSet<>();

        List<Artist> allart;
        List<SavedTrack> allsong;

        allart = FileMethods.listFromJson(context, ARTIST_DATABASE, Artist.class);
        allsong = FileMethods.listFromJson(context, SONG_DATABASE, SavedTrack.class);

        for (SavedTrack elem : allsong){
            Track song = elem.track;
            List<String> artists = new ArrayList<>();
            List<String> artistIDs = new ArrayList<>();
            List<String> genres = new ArrayList<>();
            for (ArtistSimple art : song.artists) {
                artists.add(art.name);
                artistIDs.add(art.id);
                for (Artist a : allart)
                {
                    if (a.id.equals(art.id))
                    {
                        genres.addAll(a.genres); // The genre must be queried via the artist
                        // database because tracks don't have genres in spotify and unfortunately
                        // the albums also often don't implemented the genres.
                    }
                }
            }
            songs.add(new MyTrack(song.name, song.id, artists, artistIDs, genres));
        }
        return songs;
    }
    /**
     * Saves all FavouriteSongs in one JSON Datei: SONG_DATABASE
     */
    public void create_fav_database() {
        List<SavedTrack> allTracks = new ArrayList<>();

        spotify.getMySavedTracks(new SpotifyCallback<>() {
            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                Log.d("Anfrage", "Erfolgreich");
                int len = savedTrackPager.total;

                for (int i = 0; i < len; i += 50) {
                    spotify.getMySavedTracks(Map.of("market", "DE", "limit", 50, "offset", i), new SpotifyCallback<>() {
                        @Override
                        public void success(Pager<SavedTrack> savedTrackPager2, Response response) {
                            List<SavedTrack> myTracks = savedTrackPager2.items;
                            allTracks.addAll(myTracks);
                            if (allTracks.size() == len) // After all requests
                            {
                                FileMethods.inFile(context, SONG_DATABASE, allTracks);

                                // all artist IDs in a Set
                                Set<String> artIDs = new HashSet<>();
                                for (SavedTrack k : allTracks)
                                {
                                    for (ArtistSimple as : k.track.artists)
                                        artIDs.add(as.id);
                                }

                                create_artist_database(artIDs, ARTIST_DATABASE);

                                long timestamp = System.currentTimeMillis();
                                msharedPreferences.edit().putLong("time-favourite", timestamp).apply();
                                Log.d("ANFRAGE", String.valueOf(timestamp));
                            }
                        }

                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.e("REQUEST", spotifyError.toString());
                        }
                    });
                }
            }

            @Override
            public void failure(SpotifyError spotifyError) {
                Log.e("REQUEST", spotifyError.toString());
            }
        });
    }


    /**
     * Creates a JSON File from a Set of artist IDs
     * @param artist_ids Die IDs der Artists
     * @param filename Der Filename
     */
    public void create_artist_database(Set<String> artist_ids, String filename) {
        List<Artist> allArtists = new ArrayList<>();
        String [] art_arr = artist_ids.toArray(new String[0]); //Set to arr
        int len = art_arr.length; //length

        for (int j = 0; j<len; j+=50){// steps in size of 50, because its the maximum of 1 request
            int end = j+50;    //end of the inner for loop
            if (end>len)
            {
                end = len;     //not array out of bounds
            }
            StringBuilder all = new StringBuilder(); //String Builder, because
            // the Ids need to be in a comma separated String

            all.append(art_arr[j]);     //before first element no comma
            for (int i = j+1; i<end; i++)
            {
                all.append(",").append(art_arr[i]); //else comma separated
            }
            spotify.getArtists(all.toString(), new Callback<>() {
                @Override
                public void success(Artists artists, Response response) {
                    allArtists.addAll(artists.artists); //append request to all
                    if (allArtists.size() == len)   // after last request
                    {
                        FileMethods.inFile(context, filename, allArtists); // write in file
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e("ARTIST_DATABASE", error.toString());
                }
            });
        }

    }
}
