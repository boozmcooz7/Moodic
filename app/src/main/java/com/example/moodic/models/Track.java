package com.example.moodic.models;

public class Track {
    private String youtubeId;

    private String title;
    private String artist;
    private int durationSeconds;

    // מודל ה-Track צריך גם Constructor ריק ו-Getters/Setters:

    public Track() {
    }

    public Track(String youtubeId, String title, String artist, int durationSeconds) {
        this.youtubeId = youtubeId;
        this.title = title;
        this.artist = artist;
        this.durationSeconds = durationSeconds;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public void setYoutubeId(String youtubeId) {
        this.youtubeId = youtubeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}