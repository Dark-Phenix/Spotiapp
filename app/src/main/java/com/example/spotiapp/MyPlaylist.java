package com.example.spotiapp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class MyPlaylist {
    private String name;
    private final String id;
    private String description;
    private String image;
    private Set<String> genres;
    private List<String> artists;
    private LocalDateTime lastUpdated;


    public MyPlaylist(String name, String id, String description, String image, Set<String> genres, List<String> artists, LocalDateTime lastUpdated) {
        this.name = name;
        this.id = id;
        this.description = description;
        this.image = image;
        this.genres = genres;
        this.artists = artists;
        this.lastUpdated = lastUpdated;
    }


    @Override
    public String toString() {
        return "MyPlaylist{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", image='" + image + '\'' +
                ", genres=" + genres +
                ", artists=" + artists +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Set<String> getGenres() {
        return genres;
    }

    public void setGenres(Set<String> genres) {
        this.genres = genres;
    }

    public List<String> getArtists() {
        return artists;
    }

    public void setArtists(List<String> artists) {
        this.artists = artists;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
