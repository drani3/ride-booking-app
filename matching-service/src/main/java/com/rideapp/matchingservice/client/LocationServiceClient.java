package com.rideapp.matchingservice.client;

import com.rideapp.matchingservice.dto.DriverResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "location-service", url = "${location.service.url}")
public interface LocationServiceClient {

    @GetMapping("/api/v1/location/drivers/nearby")
    List<DriverResponse> getNearByDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double radius
    );
}