package com.rideapp.locationservice.dto;

public class ReverseGeocodingRequest {
    private double latitude;
    private double longitude;

    public ReverseGeocodingRequest() {
    }

    public ReverseGeocodingRequest(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
