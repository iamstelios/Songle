package com.iamstelios.songle;

/**
 * Class used to define the different placemarks used on the map of the game.
 * <p>Placemarks represent a lyric of the current song</p>
 * <p>All properties are set in the constructor and cannot be changed afterwards.</p>
 */
public class Placemark {
    private final String name;
    private final String description;
    private final double latitude;
    private final double longitude;

    public Placemark(String name, String description, double latitude, double longitude) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    //Getter methods for accessing the properties of a Placemark
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}