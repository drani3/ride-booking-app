package com.rideapp.locationservice.controller;

import com.rideapp.locationservice.dto.DriverResponse;
import com.rideapp.locationservice.dto.Driver;
import com.rideapp.locationservice.dto.DirectionsRequest;
import com.rideapp.locationservice.dto.DirectionsResponse;
import com.rideapp.locationservice.dto.GeocodingRequest;
import com.rideapp.locationservice.dto.GeocodingResponse;
import com.rideapp.locationservice.dto.ReverseGeocodingRequest;
import com.rideapp.locationservice.service.DirectionsService;
import com.rideapp.locationservice.service.GeocodingService;
import com.rideapp.locationservice.service.LocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequestMapping("/api/v1/location")
@Slf4j
@RestController
public class LocationController{

    // Set by the API gateway from the verified JWT; clients cannot supply them
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final LocationService locationService;
    private final GeocodingService geocodingService;
    private final DirectionsService directionsService;

    public LocationController(LocationService locationService, GeocodingService geocodingService, DirectionsService directionsService) {
        this.locationService = locationService;
        this.geocodingService = geocodingService;
        this.directionsService = directionsService;
    }

    @PostMapping("/drivers/update")
    public ResponseEntity<String> updateDriverLocation(
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestHeader(USER_ROLE_HEADER) String role,
            @RequestBody Driver driver){
        requireDriver(role);
        // Drivers can only move themselves, whatever driverId the body says
        driver.setDriverId(userId);
        locationService.updateDriverLocation(driver);
        return ResponseEntity.ok("Driver Location updated");
    }

    //Matching service calls this when ride is requested
    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<DriverResponse>> fetchDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam (defaultValue = "5.0") double radius) {

        return ResponseEntity.ok(locationService.fetchDrivers(latitude, longitude, radius));
    }

    // called when driver goes offline
    @DeleteMapping("/drivers/{driverID}")
    public ResponseEntity<String> removeDriver(
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestHeader(USER_ROLE_HEADER) String role,
            @PathVariable String driverID){
        requireDriver(role);
        if (!driverID.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Drivers can only take themselves offline");
        }
        locationService.removeDriver(driverID);
        return ResponseEntity.ok("Driver removed successfully");
    }

    @PostMapping("/drivers/{driverId}/assign")
    public boolean assignDriver(@PathVariable String driverId) {
        return locationService.assignDriver(driverId);
    }

    @PostMapping("/drivers/{driverId}/available")
    public void markDriverAvailable(
            @PathVariable String driverId) {
        locationService.markDriverAvailable(driverId);
    }

    @PostMapping("/geocode")
    public ResponseEntity<GeocodingResponse> geocodeAddress(
            @RequestBody GeocodingRequest request) {
        GeocodingResponse response = geocodingService.geocodeAddress(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reverse-geocode")
    public ResponseEntity<GeocodingResponse> reverseGeocode(
            @RequestBody ReverseGeocodingRequest request) {
        GeocodingResponse response = geocodingService.reverseGeocode(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateAddress(
            @RequestParam String address) {
        boolean isValid = geocodingService.validateAddress(address);
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/directions")
    public ResponseEntity<DirectionsResponse> calculateDirections(
            @RequestBody DirectionsRequest request) {
        DirectionsResponse response = directionsService.calculateRoute(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/eta")
    public ResponseEntity<Long> calculateETA(
            @RequestParam double currentLat,
            @RequestParam double currentLng,
            @RequestParam double destLat,
            @RequestParam double destLng) {
        long eta = directionsService.calculateETA(currentLat, currentLng, destLat, destLng);
        return ResponseEntity.ok(eta);
    }

    private void requireDriver(String role) {
        if (!"DRIVER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only drivers can do this");
        }
    }
}
