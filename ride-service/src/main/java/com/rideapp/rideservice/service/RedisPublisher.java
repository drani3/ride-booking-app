package com.rideapp.rideservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideapp.rideservice.config.RedisConfig;
import com.rideapp.rideservice.dto.DriverLocationUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishDriverLocation(DriverLocationUpdate update) {
        try {
            String message = objectMapper.writeValueAsString(update);
            redisTemplate.convertAndSend(RedisConfig.DRIVER_LOCATION_TOPIC, message);
            log.debug("Published driver location update to Redis for ride {}", update.getRideId());
        } catch (Exception e) {
            log.error("Error publishing to Redis", e);
        }
    }
}
