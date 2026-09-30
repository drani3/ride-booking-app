package com.rideapp.rideservice.dto;

import java.util.List;

public class RouteUpdate {
    private String rideId;
    private String encodedPolyline;
    private double distanceMeters;
    private double durationSeconds;
    private List<RouteStep> steps;

    public RouteUpdate() {
    }

    public RouteUpdate(String rideId, String encodedPolyline, double distanceMeters, double durationSeconds, List<RouteStep> steps) {
        this.rideId = rideId;
        this.encodedPolyline = encodedPolyline;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.steps = steps;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getEncodedPolyline() {
        return encodedPolyline;
    }

    public void setEncodedPolyline(String encodedPolyline) {
        this.encodedPolyline = encodedPolyline;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public List<RouteStep> getSteps() {
        return steps;
    }

    public void setSteps(List<RouteStep> steps) {
        this.steps = steps;
    }

    public static class RouteStep {
        private String instruction;
        private double distanceMeters;
        private double durationSeconds;

        public RouteStep() {
        }

        public RouteStep(String instruction, double distanceMeters, double durationSeconds) {
            this.instruction = instruction;
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public double getDistanceMeters() {
            return distanceMeters;
        }

        public void setDistanceMeters(double distanceMeters) {
            this.distanceMeters = distanceMeters;
        }

        public double getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(double durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }
}
