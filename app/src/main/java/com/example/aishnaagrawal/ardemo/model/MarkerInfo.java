package com.example.aishnaagrawal.ardemo.model;

import android.location.Location;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by aishnaagrawal on 11/16/17.
 */

public class MarkerInfo {

    @SerializedName("_id")
    @Expose
    private String id;
    @SerializedName("time")
    @Expose
    private LocationTime time;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("category")
    @Expose
    private String category;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("location")
    @Expose
    private MarkerLocation markerLocation;

    private Float distance;

    private Boolean inRange = false;

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public Boolean getInRange() {
        return inRange;
    }

    public void setInRange(Boolean inRange) {
        this.inRange = inRange;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocationTime getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public Location getLocation() {
        Location location = new Location(name);
        location.setLatitude(markerLocation.getLat());
        location.setLongitude(markerLocation.getLng());
        return location;
    }


}

