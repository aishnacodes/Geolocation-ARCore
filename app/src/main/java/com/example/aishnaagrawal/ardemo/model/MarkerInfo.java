package com.example.aishnaagrawal.ardemo.model;

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
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("location")
    @Expose
    private MarkerLocation markerLocation;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MarkerLocation getLocation() {
        return markerLocation;
    }

    public void setLocation(MarkerLocation location) {
        this.markerLocation = location;
    }

    public LocationTime getTime() {
        return time;
    }

    public void setTime(LocationTime time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}

