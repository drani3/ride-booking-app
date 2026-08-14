package com.rideapp.locationservice.controller;

import com.rideapp.locationservice.dto.DriverResponse;
import com.rideapp.locationservice.service.LocationService;
import com.rideapp.locationservice.dto.Driver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/location")
@Slf4j
@RestController
public class LocationController{

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/drivers/update")
    public ResponseEntity<String> updateDriverLocation(
            @RequestBody Driver driver){
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
    public ResponseEntity<String> removeDriver(@PathVariable String driverID){
        locationService.removeDriver(driverID);
        return ResponseEntity.ok("Driver removed successfully");
    }



}