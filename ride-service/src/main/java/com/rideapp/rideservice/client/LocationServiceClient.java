package com.rideapp.rideservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "location-service", url = "${location.service.url}")
public interface LocationServiceClient {

    @PostMapping("/api/v1/location/drivers/{driverId}/available")
    void markDriverAvailable(
            @PathVariable String driverId
    );
}