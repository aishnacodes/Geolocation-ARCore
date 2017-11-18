package com.example.aishnaagrawal.ardemo.model;

import android.location.Location;

/**
 * Created by aishnaagrawal on 11/2/17.
 */

public class ARPoint {
    Location location;
    String name;

    public ARPoint(String name, double lat, double lon, double altitude) {
        this.name = name;
        location = new Location("ARPoint");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAltitude(altitude);
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
}
