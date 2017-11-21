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

    public MarkerLocation(String latitude, String longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public float getLat() {
        return Float.parseFloat(latitude);
    }

    public float getLng() {
        return Float.parseFloat(longitude);
    }
}
