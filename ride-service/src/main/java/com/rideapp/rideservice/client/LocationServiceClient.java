package com.rideapp.rideservice.client;

import com.rideapp.rideservice.dto.GeocodingRequest;
import com.rideapp.rideservice.dto.GeocodingResponse;
import com.rideapp.rideservice.dto.ReverseGeocodingRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "location-service", url = "${location.service.url}")
public interface LocationServiceClient {

    @PostMapping("/api/v1/location/drivers/{driverId}/available")
    void markDriverAvailable(
            @PathVariable String driverId
    );

    @PostMapping("/api/v1/location/geocode")
    GeocodingResponse geocodeAddress(
            @RequestBody GeocodingRequest request
    );

    @PostMapping("/api/v1/location/reverse-geocode")
    GeocodingResponse reverseGeocode(
            @RequestBody ReverseGeocodingRequest request
    );

    @GetMapping("/api/v1/location/validate")
    Boolean validateAddress(
            @RequestParam String address
    );
}