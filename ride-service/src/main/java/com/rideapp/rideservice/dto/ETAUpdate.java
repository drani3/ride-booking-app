package com.rideapp.rideservice.dto;

public class ETAUpdate {
    private String rideId;
    private long estimatedArrivalTime;
    private double distanceRemainingMeters;

    public ETAUpdate() {
    }

    public ETAUpdate(String rideId, long estimatedArrivalTime, double distanceRemainingMeters) {
        this.rideId = rideId;
        this.estimatedArrivalTime = estimatedArrivalTime;
        this.distanceRemainingMeters = distanceRemainingMeters;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public long getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }

    public void setEstimatedArrivalTime(long estimatedArrivalTime) {
        this.estimatedArrivalTime = estimatedArrivalTime;
    }

    public double getDistanceRemainingMeters() {
        return distanceRemainingMeters;
    }

    public void setDistanceRemainingMeters(double distanceRemainingMeters) {
        this.distanceRemainingMeters = distanceRemainingMeters;
    }
}
