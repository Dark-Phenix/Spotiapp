package com.example.spotiapp;



import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    public static final String GENRE_DATABASE = "artist_genres.json";


    private static final long CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000; // 24 Stunden
    //TODO statt automatisch nach mehr als 24 stunden komplett neu zu machen, lieber immer
    //wieder ab letzten mal schauen welche seit dem dazu gekommen sind und dann appenden
    private final SpotifyService spotify;

    private final Context context;

    private SharedPreferences msharedPreferences;
    private FavDatabaseListener listener;

    public void setFavDatabaseListener(FavDatabaseListener listener) {
        this.listener = listener;
    }


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
     * Ensures that the favourite songs database is available.
     * If the file already exists and is recent enough, the existing file is used and the listener is triggered.
     * Otherwise, the database is recreated via {@link #create_fav_database_async()}.
     *
     * @return A CompletableFuture that completes when the database is ready.
     */
    public CompletableFuture<Void> ensureFavDatabaseAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        boolean fileExists = FileMethods.fileExists(context, SONG_DATABASE);
        long lastUpdated = msharedPreferences.getLong("time-favourite", 0);
        long now = System.currentTimeMillis();
        long cacheDuration = 1000L * 60 * 60 * 12; // 12 Stunden

        if (fileExists && (now - lastUpdated) < cacheDuration) {
            Log.d("DATABASE", "Song-DB bereits vorhanden, Listener wird direkt getriggert");

            // Falls Genres fehlen, trotzdem generieren
            maybeGenerateGenres();

            if (listener != null) {
                listener.onFavDatabaseReady();
            }

            future.complete(null);
        } else {
            create_fav_database_async().thenRun(() -> {
                maybeGenerateGenres();

                // Der Listener wird bereits in create_fav_database_async() getriggert
                future.complete(null);
            }).exceptionally(ex -> {
                future.completeExceptionally(ex);
                return null;
            });
        }

        return future;
    }

    // Diese neue Methode prüft nur, ob artist_genres.json fehlt, und erzeugt sie dann
    private void maybeGenerateGenres() {
        if (!FileMethods.fileExists(context, GENRE_DATABASE)) {
            Log.d("DATABASE", "Genre-DB fehlt – wird aus Artist-DB erzeugt.");
            loadAndCacheGenresFromArtistDatabase();
        } else {
            Log.d("DATABASE", "Genre-DB bereits vorhanden.");
        }
    }



    /**
     * Prüft, ob die Künstler-Datenbank-Datei existiert und der gespeicherte Cache-Timestamp noch gültig ist.
     * Wenn ja, wird keine neue Spotify-Abfrage gemacht und ein sofort abgeschlossenes CompletableFuture zurückgegeben.
     * Wenn nein, wird die Methode {@link #create_artist_database_async(Set, String)} aufgerufen, um die Daten neu zu laden und zu speichern.
     *
     * Nach erfolgreichem Abschluss wird der Timestamp in SharedPreferences aktualisiert.
     *
     * @param artistIDs Menge der Künstler-IDs, die abgefragt werden sollen.
     * @return CompletableFuture, das abgeschlossen wird, wenn die Künstler-Datenbank aktuell vorliegt oder neu erstellt wurde.
     */
    public CompletableFuture<Void> ensureArtistDatabaseAsync(Set<String> artistIDs) {
        boolean fileExists = FileMethods.fileExists(context, ARTIST_DATABASE);
        long lastFetchTime = msharedPreferences.getLong("time-artist", 0);
        long now = System.currentTimeMillis();

        if (fileExists && (now - lastFetchTime) < CACHE_VALIDITY_MS) {
            return CompletableFuture.completedFuture(null);
        } else {
            return create_artist_database_async(artistIDs, ARTIST_DATABASE)
                    .thenRun(() -> msharedPreferences.edit().putLong("time-artist", now).apply());
        }
    }


    /**
     * Saves all FavouriteSongs in one JSON Datei: SONG_DATABASE
     */
    private CompletableFuture<Void> create_fav_database_async() {
        CompletableFuture<Void> overallFuture = new CompletableFuture<>();
        List<SavedTrack> allTracks = Collections.synchronizedList(new ArrayList<>());

        spotify.getMySavedTracks(new SpotifyCallback<>() {
            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                int total = savedTrackPager.total;
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = 0; i < total; i += 50) {
                    int offset = i;
                    CompletableFuture<Void> future = new CompletableFuture<>();

                    spotify.getMySavedTracks(Map.of("market", "DE", "limit", 50, "offset", offset), new SpotifyCallback<>() {
                        @Override
                        public void success(Pager<SavedTrack> page, Response response) {
                            allTracks.addAll(page.items);
                            future.complete(null);
                        }

                        @Override
                        public void failure(SpotifyError error) {
                            future.completeExceptionally(error);
                        }
                    });

                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenRun(() -> {
                            FileMethods.inFile(context, SONG_DATABASE, allTracks);

                            // artist IDs sammeln
                            Set<String> artistIDs = allTracks.stream()
                                    .flatMap(track -> track.track.artists.stream())
                                    .map(artist -> artist.id)
                                    .collect(Collectors.toSet());

                            create_artist_database_async(artistIDs, ARTIST_DATABASE)
                                    .thenRun(() -> {
                                        msharedPreferences.edit().putLong("time-favourite", System.currentTimeMillis()).apply();

                                        // Listener aufrufen, wenn alles fertig ist
                                        if (listener != null) {
                                            listener.onFavDatabaseReady();
                                        }
                                        overallFuture.complete(null);
                                    })
                                    .exceptionally(ex -> {
                                        overallFuture.completeExceptionally(ex);
                                        return null;
                                    });
                        })
                        .exceptionally(ex -> {
                            overallFuture.completeExceptionally(ex);
                            return null;
                        });
            }

            @Override
            public void failure(SpotifyError error) {
                overallFuture.completeExceptionally(error);
            }
        });

        return overallFuture;
    }


    private CompletableFuture<Void> create_artist_database_async(Set<String> artist_ids, String filename) {
        List<Artist> allArtists = Collections.synchronizedList(new ArrayList<>());
        String[] art_arr = artist_ids.toArray(new String[0]);
        int len = art_arr.length;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int j = 0; j < len; j += 50) {
            int end = Math.min(j + 50, len);
            StringBuilder all = new StringBuilder();
            all.append(art_arr[j]);
            for (int i = j + 1; i < end; i++) {
                all.append(",").append(art_arr[i]);
            }

            String idsParam = all.toString();
            CompletableFuture<Void> future = new CompletableFuture<>();

            spotify.getArtists(idsParam, new Callback<>() {
                @Override
                public void success(Artists artists, Response response) {
                    allArtists.addAll(artists.artists);
                    future.complete(null);
                }

                @Override
                public void failure(RetrofitError error) {
                    future.completeExceptionally(error);
                }
            });

            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> FileMethods.inFile(context, filename, allArtists));
    }

    public Set<String> loadAndCacheGenresFromArtistDatabase() {
        List<Artist> allArtists = FileMethods.listFromJson(context, ARTIST_DATABASE, Artist.class);
        if (allArtists == null || allArtists.isEmpty()) {
            Log.w("Favourite", "Artist database is empty or missing.");
            return Collections.emptySet();
        }

        Set<String> allGenres = new HashSet<>();
        for (Artist artist : allArtists) {
            if (artist.genres != null) {
                allGenres.addAll(artist.genres);
            }
        }

        // Genres als Liste speichern (JSON-Serialisierung braucht oft Liste statt Set)
        List<String> genreList = new ArrayList<>(allGenres);
        FileMethods.inFile(context, GENRE_DATABASE, genreList);

        Log.d("Favourite", "Collected and cached " + allGenres.size() + " unique genres.");

        return allGenres;
    }

    public Set<String> loadGenresFromCache() {
        List<String> cachedGenres = FileMethods.listFromJson(context, GENRE_DATABASE, String.class);
        if (cachedGenres == null) {
            Log.w("Favourite", "Genre cache file missing or empty.");
            return Collections.emptySet();
        }
        return new HashSet<>(cachedGenres);
    }

    public Set<String> getGenres(boolean forceReload) {
        if (forceReload || !FileMethods.fileExists(context, GENRE_DATABASE)) {
            // Neu laden und cachen
            return loadAndCacheGenresFromArtistDatabase();
        } else {
            // Aus Cache laden
            return loadGenresFromCache();
        }
    }


}
