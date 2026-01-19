
package com.example.moodic.models;

import java.util.List;

public class User {
    private String uid;
    private String userName;
    private List<String> favoriteGenres;
    private List<Track> favoriteTracks;
    private double[] dynamicListeningProfile;
    private String lastUpdated;

    public User() {
    }

    public User(String uid, String userName, List<String> favoriteGenres, List<Track> favoriteTracks, double[] dynamicListeningProfile) {
        this.uid = uid;
        this.userName = userName;
        this.favoriteGenres = favoriteGenres;
        this.favoriteTracks = favoriteTracks;
        this.dynamicListeningProfile = dynamicListeningProfile;
        this.lastUpdated = lastUpdated;
    }


    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public void setFavoriteGenres(List<String> favoriteGenres) {
        this.favoriteGenres = favoriteGenres;
    }

    public List<Track> getFavoriteTracks() {
        return favoriteTracks;
    }

    public void setFavoriteTracks(List<Track> favoriteTracks) {
        this.favoriteTracks = favoriteTracks;
    }

    public double[] getDynamicListeningProfile() {
        return dynamicListeningProfile;
    }

    public void setDynamicListeningProfile(double[] dynamicListeningProfile) {
        this.dynamicListeningProfile = dynamicListeningProfile;
    }
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
}
