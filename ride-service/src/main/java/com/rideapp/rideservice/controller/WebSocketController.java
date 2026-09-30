package com.rideapp.rideservice.controller;

import com.rideapp.rideservice.dto.DriverLocationUpdate;
import com.rideapp.rideservice.service.LocationTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class WebSocketController {

    private final LocationTrackingService locationTrackingService;

    @MessageMapping("/driver/location")
    public void handleDriverLocationUpdate(@Payload DriverLocationUpdate update) {
        log.info("Received driver location update for ride {}: lat={}, lng={}", 
            update.getRideId(), update.getLatitude(), update.getLongitude());
        
        locationTrackingService.broadcastDriverLocation(update);
    }
}
