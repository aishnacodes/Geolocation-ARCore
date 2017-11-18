package com.example.aishnaagrawal.ardemo.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by aishnaagrawal on 11/16/17.
 */

public class MarkerLocation {
    @SerializedName("lat")
    @Expose
    private String latitude;
    @SerializedName("lng")
    @Expose
    private String longitude;

    public String getLat() {
        return latitude;
    }

    public void setLat(String lat) {
        this.latitude = lat;
    }

    public String getLng() {
        return longitude;
    }

    public void setLng(String lng) {
        this.longitude = lng;
    }
}
