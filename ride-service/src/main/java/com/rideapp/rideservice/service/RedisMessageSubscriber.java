package com.rideapp.rideservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideapp.rideservice.dto.DriverLocationUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisMessageSubscriber {

    private final LocationTrackingService locationTrackingService;
    private final ObjectMapper objectMapper;

    public void handleMessage(String message) {
        try {
            DriverLocationUpdate update = objectMapper.readValue(message, DriverLocationUpdate.class);
            locationTrackingService.broadcastDriverLocation(update);
            log.debug("Received and broadcasted driver location update from Redis for ride {}", update.getRideId());
        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }
}
