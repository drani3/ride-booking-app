package com.rideapp.locationservice.dto;

import java.util.List;

public class DirectionsResponse {
    private double distanceMeters;
    private double durationSeconds;
    private String encodedPolyline;
    private List<RouteStep> steps;
    private boolean isValid;
    private String errorMessage;

    public DirectionsResponse() {
    }

    public DirectionsResponse(double distanceMeters, double durationSeconds, String encodedPolyline, List<RouteStep> steps, boolean isValid, String errorMessage) {
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.encodedPolyline = encodedPolyline;
        this.steps = steps;
        this.isValid = isValid;
        this.errorMessage = errorMessage;
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

    public String getEncodedPolyline() {
        return encodedPolyline;
    }

    public void setEncodedPolyline(String encodedPolyline) {
        this.encodedPolyline = encodedPolyline;
    }

    public List<RouteStep> getSteps() {
        return steps;
    }

    public void setSteps(List<RouteStep> steps) {
        this.steps = steps;
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
