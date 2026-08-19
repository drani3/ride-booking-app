package com.rideapp.matchingservice.service;

import com.rideapp.matchingservice.client.LocationServiceClient;
import com.rideapp.matchingservice.dto.DriverResponse;
import com.rideapp.matchingservice.event.RideMatchedEvent;
import com.rideapp.matchingservice.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final LocationServiceClient locationServiceClient;
    private final KafkaTemplate<String, RideMatchedEvent> kafkaTemplate;

    private static final String RIDE_MATCHED_TOPIC = "ride.matched";
    private static final double DEFAULT_SEARCH_RADIUS_KM = 5.0;

    public void matchDriverForRide(RideRequestedEvent event){

        List<DriverResponse> nearByDrivers = locationServiceClient.getNearByDrivers(
                event.getPickupLatitude(),
                event.getPickupLongitude(),
                DEFAULT_SEARCH_RADIUS_KM
        );

        if(nearByDrivers.isEmpty()){
            log.warn("No drivers found near ride");
            return;
        }

        List<DriverResponse> bestDrivers = findBestDrivers(nearByDrivers);

        for(DriverResponse driver : bestDrivers){
            boolean assigned =
                    locationServiceClient.assignDriver(
                            driver.getDriverId()
                    );

            if (assigned) {
                RideMatchedEvent matchedEvent = new RideMatchedEvent(
                        event.getRideId(),
                        event.getRiderId(),
                        driver.getDriverId(),
                        driver.getLatitude(),
                        driver.getLongitude(),
                        driver.getDistance()
                );

                kafkaTemplate.send(RIDE_MATCHED_TOPIC, event.getRideId(), matchedEvent);
                log.info("RideMatchedEvent published");
                return;
            }
        }

        log.warn("could not find suitable driver for ride");
    }

    private List<DriverResponse> findBestDrivers(
            List<DriverResponse> drivers) {

        double distanceWeight = 0.7;
        double ratingWeight = 0.3;

        return drivers.stream()
                .sorted(Comparator.comparingDouble((DriverResponse driver) -> {

                    double distanceScore =
                            1.0 / (driver.getDistance() + 0.1);

                    double simulatedRating =
                            4.0 + Math.random();

                    return (distanceScore * distanceWeight)
                            + (simulatedRating * ratingWeight);

                }).reversed())
                .toList();
    }
}