package com.rideapp.locationservice.dto;

public class GeocodingRequest {
    private String address;

    public GeocodingRequest() {
    }

    public GeocodingRequest(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
