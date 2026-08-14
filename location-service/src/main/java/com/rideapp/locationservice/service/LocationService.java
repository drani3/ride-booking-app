package com.rideapp.locationservice.service;

import com.rideapp.locationservice.dto.Driver;
import com.rideapp.locationservice.dto.DriverResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.geo.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final RedisTemplate<String, String> redisTemplate;

    //Redis key for all driver locations
    private static final String DRIVERS_GEO_KEY = "drivers:locations";

    /**
     * Update driver location in Redis.
     * Called every 3 seconds by driver's phone
     * Maps to Redis GEOADD command
     */

    public void updateDriverLocation(Driver driver){
        log.info("Updating location for driver: {}", driver.getDriverId());

        // IMPORTANT: longitude FIRST, latitude SECOND - GeoSpatial Standard
        Point driverPoint = new Point(
                driver.getLongitude(),
                driver.getLatitude()
        );

        redisTemplate.opsForGeo().add(
                DRIVERS_GEO_KEY,
                driverPoint,
                driver.getDriverId()
        );

        log.info("Location updated for driver: {}", driver.getDriverId());
    }

    /**
     * Find nearby drivers within given radius.
     * Called by Matching Service on ride request.
     * Maps to Redis FEORADIUS command.
     */

    public List<DriverResponse> findNearbyDrivers(
            double latitude, double longitude, double radiusInKm) {

        log.info("Finding drivers near lat: {} long: {} withing {}Km",
                latitude, longitude, radiusInKm);

        Circle searchArea = new Circle(
                new Point(longitude, latitude),
                new Distance(radiusInKm, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(
                        DRIVERS_GEO_KEY,
                        searchArea,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeCoordinates()
                                .includeDistance()
                                .sortAscending()
                                .limit(10)
                );

        List<DriverResponse> nearbyDrivers = new ArrayList<>();

        if(results != null){
            results.getContent().forEach(result -> {
                RedisGeoCommands.GeoLocation<String> location = result.getContent();
                nearbyDrivers.add(new DriverResponse(
                        location.getName(),
                        location.getPoint().getY(),
                        location.getPoint().getX(),
                        result.getDistance().getValue()
                ));
            });
        }
        log.info("Found {} drivers nearby", nearbyDrivers.size());
        return nearbyDrivers;
    }

    /**
     * Remove driver when they go offline
     * Maps to Redis ZREM command.
     */

    public void removeDriver(String driverId){
        log.info("Removing driver: {}", driverId);
        redisTemplate.opsForGeo().remove(DRIVERS_GEO_KEY, driverId);
    }
}