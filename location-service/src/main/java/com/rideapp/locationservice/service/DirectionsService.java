package com.rideapp.locationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.DirectionsApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.LatLng;
import com.rideapp.locationservice.dto.DirectionsRequest;
import com.rideapp.locationservice.dto.DirectionsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DirectionsService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    @Value("${google.maps.directions.cache.ttl:3600}")
    private long cacheTtlSeconds;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DIRECTIONS_CACHE_PREFIX = "directions:";

    private GeoApiContext getGeoApiContext() {
        return new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

    private String getCacheKey(double originLat, double originLng, double destLat, double destLng) {
        return DIRECTIONS_CACHE_PREFIX + String.format("%.6f,%.6f-%.6f,%.6f", originLat, originLng, destLat, destLng);
    }

    public DirectionsResponse calculateRoute(DirectionsRequest request) {
        String cacheKey = getCacheKey(
                request.getOriginLatitude(),
                request.getOriginLongitude(),
                request.getDestinationLatitude(),
                request.getDestinationLongitude()
        );

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                DirectionsResponse cachedResponse = objectMapper.readValue(cached, DirectionsResponse.class);
                log.info("Cache hit for directions");
                return cachedResponse;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached directions result", e);
            }
        }

        DirectionsResponse response = new DirectionsResponse();

        try {
            GeoApiContext context = getGeoApiContext();
            LatLng origin = new LatLng(request.getOriginLatitude(), request.getOriginLongitude());
            LatLng destination = new LatLng(request.getDestinationLatitude(), request.getDestinationLongitude());

            DirectionsResult result = DirectionsApi.newRequest(context)
                    .origin(origin)
                    .destination(destination)
                    .await();

            if (result != null && result.routes != null && result.routes.length > 0) {
                var route = result.routes[0];
                var leg = route.legs[0];

                response.setDistanceMeters(leg.distance.inMeters);
                response.setDurationSeconds(leg.duration.inSeconds);
                response.setEncodedPolyline(route.overviewPolyline.getEncodedPath());

                List<DirectionsResponse.RouteStep> steps = new ArrayList<>();
                for (var step : leg.steps) {
                    steps.add(new DirectionsResponse.RouteStep(
                            step.htmlInstructions,
                            step.distance.inMeters,
                            step.duration.inSeconds
                    ));
                }
                response.setSteps(steps);
                response.setIsValid(true);

                try {
                    String responseJson = objectMapper.writeValueAsString(response);
                    redisTemplate.opsForValue().set(cacheKey, responseJson, Duration.ofSeconds(cacheTtlSeconds));
                    log.info("Cached directions result");
                } catch (Exception e) {
                    log.warn("Failed to cache directions result", e);
                }

                log.info("Successfully calculated route: distance: {}m, duration: {}s",
                    leg.distance.inMeters, leg.duration.inSeconds);
            } else {
                response.setIsValid(false);
                response.setErrorMessage("No route found");
                log.warn("No directions result found");
            }

        } catch (ApiException e) {
            response.setIsValid(false);
            response.setErrorMessage("API error: " + e.getMessage());
            log.error("Google Maps Directions API error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setIsValid(false);
            response.setErrorMessage("Request interrupted");
            log.error("Directions request interrupted", e);
        } catch (IOException e) {
            response.setIsValid(false);
            response.setErrorMessage("IO error: " + e.getMessage());
            log.error("IO error during directions calculation", e);
        }

        return response;
    }

    public long calculateETA(double currentLat, double currentLng, double destLat, double destLng) {
        DirectionsRequest request = new DirectionsRequest(currentLat, currentLng, destLat, destLng);
        DirectionsResponse response = calculateRoute(request);
        return response.isValid() ? (long) response.getDurationSeconds() : -1;
    }
}
