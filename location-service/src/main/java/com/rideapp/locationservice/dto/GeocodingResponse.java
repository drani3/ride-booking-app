package com.rideapp.locationservice.dto;

public class GeocodingResponse {
    private double latitude;
    private double longitude;
    private String formattedAddress;
    private boolean isValid;
    private String errorMessage;

    public GeocodingResponse() {
    }

    public GeocodingResponse(double latitude, double longitude, String formattedAddress, boolean isValid, String errorMessage) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.formattedAddress = formattedAddress;
        this.isValid = isValid;
        this.errorMessage = errorMessage;
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

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
