package com.iamstelios.songle;

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