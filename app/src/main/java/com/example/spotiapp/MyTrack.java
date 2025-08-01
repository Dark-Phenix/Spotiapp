package com.example.spotiapp;

import android.support.annotation.NonNull;

import java.util.List;

public class MyTrack {

    public String name;
    public String id;
    public List<String> artistNames;
    public List<String> artistIDs;
    public List<String> genres;

    public MyTrack(String name, String id,  List<String> artistNames, List<String> artistIDs, List<String> genres) {
        this.name = name;
        this.id = id;
        this.artistNames = artistNames;
        this.artistIDs = artistIDs;
        this.genres = genres;
    }

    @NonNull
    @Override
    public String toString() {
        return "MyTrack{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", artistNames=" + artistNames +
                ", artistIDs=" + artistIDs +
                ", genres=" + genres +
                '}';
    }
}
