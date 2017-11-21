package com.iamstelios.songle;

public class Song {
    //Class for the Songs
    private final String number;
    private final String artist;
    private final String title;
    private final String link;

    public Song(String number, String artist, String title, String link) {
        this.number = number;
        this.artist = artist;
        this.title = title;
        this.link = link;
    }

    public String getNumber() {
        return number;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }
}
