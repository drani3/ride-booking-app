package com.rideapp.rideservice.dto;

public class DriverLocationUpdate {
    private String rideId;
    private String driverId;
    private double latitude;
    private double longitude;
    private double bearing;
    private double speed;
    private long timestamp;

    public DriverLocationUpdate() {
    }

    public DriverLocationUpdate(String rideId, String driverId, double latitude, double longitude, double bearing, double speed, long timestamp) {
        this.rideId = rideId;
        this.driverId = driverId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.bearing = bearing;
        this.speed = speed;
        this.timestamp = timestamp;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
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

    public double getBearing() {
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
