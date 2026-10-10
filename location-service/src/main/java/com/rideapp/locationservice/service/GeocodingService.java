package com.rideapp.locationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.rideapp.locationservice.dto.GeocodingRequest;
import com.rideapp.locationservice.dto.GeocodingResponse;
import com.rideapp.locationservice.dto.ReverseGeocodingRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeocodingService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    @Value("${google.maps.geocoding.cache.ttl:2592000}")
    private long cacheTtlSeconds;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String GEOCODE_CACHE_PREFIX = "geocode:";
    private static final String REVERSE_GEOCODE_CACHE_PREFIX = "reverse_geocode:";

    // One shared context: each GeoApiContext owns a thread pool and must be shut down
    private GeoApiContext geoApiContext;

    @PostConstruct
    void initGeoApiContext() {
        geoApiContext = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

    @PreDestroy
    void shutdownGeoApiContext() {
        geoApiContext.shutdown();
    }

    private String getCacheKey(String address) {
        return GEOCODE_CACHE_PREFIX + address.toLowerCase().replaceAll("\\s+", "_");
    }

    private String getReverseCacheKey(double lat, double lng) {
        return REVERSE_GEOCODE_CACHE_PREFIX + String.format("%.6f,%.6f", lat, lng);
    }

    /**
     * Forward geocoding: Convert address to latitude and longitude
     */
    public GeocodingResponse geocodeAddress(GeocodingRequest request) {
        String cacheKey = getCacheKey(request.getAddress());
        
        // Try to get from cache first
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                GeocodingResponse cachedResponse = objectMapper.readValue(cached, GeocodingResponse.class);
                log.info("Cache hit for address: {}", request.getAddress());
                return cachedResponse;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached geocoding result for address: {}", request.getAddress(), e);
            }
        }

        GeocodingResponse response = new GeocodingResponse();
        
        try {
            GeoApiContext context = geoApiContext;
            GeocodingResult[] results = GeocodingApi.geocode(context, request.getAddress()).await();
            
            if (results != null && results.length > 0) {
                GeocodingResult result = results[0];
                LatLng location = result.geometry.location;
                
                response.setLatitude(location.lat);
                response.setLongitude(location.lng);
                response.setFormattedAddress(result.formattedAddress);
                response.setIsValid(true);
                
                // Cache the result
                try {
                    String responseJson = objectMapper.writeValueAsString(response);
                    redisTemplate.opsForValue().set(cacheKey, responseJson, Duration.ofSeconds(cacheTtlSeconds));
                    log.info("Cached geocoding result for address: {}", request.getAddress());
                } catch (Exception e) {
                    log.warn("Failed to cache geocoding result for address: {}", request.getAddress(), e);
                }
                
                log.info("Successfully geocoded address: {} -> lat: {}, lng: {}", 
                    request.getAddress(), location.lat, location.lng);
            } else {
                response.setIsValid(false);
                response.setErrorMessage("No results found for the provided address");
                log.warn("No geocoding results found for address: {}", request.getAddress());
            }
            
        } catch (ApiException e) {
            response.setIsValid(false);
            response.setErrorMessage("API error: " + e.getMessage());
            log.error("Google Maps API error for address: {}", request.getAddress(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setIsValid(false);
            response.setErrorMessage("Request interrupted");
            log.error("Geocoding request interrupted for address: {}", request.getAddress(), e);
        } catch (IOException e) {
            response.setIsValid(false);
            response.setErrorMessage("IO error: " + e.getMessage());
            log.error("IO error during geocoding for address: {}", request.getAddress(), e);
        }
        
        return response;
    }

    /**
     * Reverse geocoding: Convert latitude and longitude to readable address
     */
    public GeocodingResponse reverseGeocode(ReverseGeocodingRequest request) {
        String cacheKey = getReverseCacheKey(request.getLatitude(), request.getLongitude());
        
        // Try to get from cache first
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                GeocodingResponse cachedResponse = objectMapper.readValue(cached, GeocodingResponse.class);
                log.info("Cache hit for coordinates: lat: {}, lng: {}", request.getLatitude(), request.getLongitude());
                return cachedResponse;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached reverse geocoding result for coordinates: lat: {}, lng: {}", 
                    request.getLatitude(), request.getLongitude(), e);
            }
        }

        GeocodingResponse response = new GeocodingResponse();
        
        try {
            GeoApiContext context = geoApiContext;
            LatLng location = new LatLng(request.getLatitude(), request.getLongitude());
            GeocodingResult[] results = GeocodingApi.reverseGeocode(context, location).await();
            
            if (results != null && results.length > 0) {
                GeocodingResult result = results[0];
                
                response.setLatitude(request.getLatitude());
                response.setLongitude(request.getLongitude());
                response.setFormattedAddress(result.formattedAddress);
                response.setIsValid(true);
                
                // Cache the result
                try {
                    String responseJson = objectMapper.writeValueAsString(response);
                    redisTemplate.opsForValue().set(cacheKey, responseJson, Duration.ofSeconds(cacheTtlSeconds));
                    log.info("Cached reverse geocoding result for coordinates: lat: {}, lng: {}", 
                        request.getLatitude(), request.getLongitude());
                } catch (Exception e) {
                    log.warn("Failed to cache reverse geocoding result for coordinates: lat: {}, lng: {}", 
                        request.getLatitude(), request.getLongitude(), e);
                }
                
                log.info("Successfully reverse geocoded coordinates: lat: {}, lng: {} -> address: {}", 
                    request.getLatitude(), request.getLongitude(), result.formattedAddress);
            } else {
                response.setIsValid(false);
                response.setErrorMessage("No results found for the provided coordinates");
                log.warn("No reverse geocoding results found for coordinates: lat: {}, lng: {}", 
                    request.getLatitude(), request.getLongitude());
            }
            
        } catch (ApiException e) {
            response.setIsValid(false);
            response.setErrorMessage("API error: " + e.getMessage());
            log.error("Google Maps API error for coordinates: lat: {}, lng: {}", 
                request.getLatitude(), request.getLongitude(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setIsValid(false);
            response.setErrorMessage("Request interrupted");
            log.error("Reverse geocoding request interrupted for coordinates: lat: {}, lng: {}", 
                request.getLatitude(), request.getLongitude(), e);
        } catch (IOException e) {
            response.setIsValid(false);
            response.setErrorMessage("IO error: " + e.getMessage());
            log.error("IO error during reverse geocoding for coordinates: lat: {}, lng: {}", 
                request.getLatitude(), request.getLongitude(), e);
        }
        
        return response;
    }

    /**
     * Validate an address by attempting to geocode it
     */
    public boolean validateAddress(String address) {
        GeocodingRequest request = new GeocodingRequest(address);
        GeocodingResponse response = geocodeAddress(request);
        return response.isValid();
    }
}
