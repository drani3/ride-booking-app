package com.rideapp.locationservice.service;

import com.rideapp.locationservice.dto.Driver;
import com.rideapp.locationservice.dto.DriverResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.geo.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String DRIVERS_GEO_KEY = "drivers:locations";
    private static final String AVAILABLE_DRIVERS_GEO_KEY =
            "drivers:available:locations";
    private static final String ASSIGN_DRIVER_SCRIPT = """
        local exists = redis.call('ZSCORE', KEYS[1], ARGV[1])

        if exists then
            redis.call('ZREM', KEYS[1], ARGV[1])
            return 1
        end

        return 0
        """;


    private final DefaultRedisScript<Long> assignDriverScript =
            new DefaultRedisScript<>(
                    ASSIGN_DRIVER_SCRIPT,
                    Long.class
            );
    public void updateDriverLocation(Driver driver){
        log.info("Updating location for driver: {}", driver.getDriverId());

        Point driverPoint = new Point(
                driver.getLongitude(),
                driver.getLatitude()
        );

        redisTemplate.opsForGeo().add(
                DRIVERS_GEO_KEY,
                driverPoint,
                driver.getDriverId()
        );

        Double score = redisTemplate.opsForZSet().score(
                AVAILABLE_DRIVERS_GEO_KEY,
                driver.getDriverId()
        );

        if (score != null) {
            redisTemplate.opsForGeo().add(
                    AVAILABLE_DRIVERS_GEO_KEY,
                    driverPoint,
                    driver.getDriverId()
            );
        }

        log.info("Location updated for driver: {}", driver.getDriverId());
    }

    public void markDriverAvailable(String driverId) {

        List<Point> positions = redisTemplate.opsForGeo()
                .position(DRIVERS_GEO_KEY, driverId);

        if (positions == null || positions.isEmpty() || positions.get(0) == null) {
            log.warn("Cannot mark driver {} available: location not found", driverId);
            return;
        }

        Point point = positions.get(0);

        redisTemplate.opsForGeo().add(
                AVAILABLE_DRIVERS_GEO_KEY,
                point,
                driverId
        );

        log.info("Driver {} is now available", driverId);
    }

    public List<DriverResponse> fetchDrivers(
            double latitude, double longitude, double radiusInKm) {

        log.info("Finding drivers near lat: {} long: {} withing {}Km",
                latitude, longitude, radiusInKm);

        Circle searchArea = new Circle(
                new Point(longitude, latitude),
                new Distance(radiusInKm, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(
                        AVAILABLE_DRIVERS_GEO_KEY,
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

    public boolean assignDriver(String driverId) {

        Long result = redisTemplate.execute(
                assignDriverScript,
                List.of(AVAILABLE_DRIVERS_GEO_KEY),
                driverId
        );

        return result == 1L;
    }

    public void removeDriver(String driverId){
        log.info("Removing driver: {}", driverId);
        redisTemplate.opsForGeo().remove(DRIVERS_GEO_KEY, driverId);
        redisTemplate.opsForGeo().remove(AVAILABLE_DRIVERS_GEO_KEY, driverId);
    }
}