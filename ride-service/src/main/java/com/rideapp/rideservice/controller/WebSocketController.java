package com.rideapp.rideservice.controller;

import com.rideapp.rideservice.dto.DriverLocationUpdate;
import com.rideapp.rideservice.model.Ride;
import com.rideapp.rideservice.model.RideStatus;
import com.rideapp.rideservice.repository.RideRepository;
import com.rideapp.rideservice.service.LocationTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Set;

@Controller
@Slf4j
@RequiredArgsConstructor
public class WebSocketController {

    private static final Set<RideStatus> TRACKABLE_STATUSES =
            Set.of(RideStatus.ACCEPTED, RideStatus.DRIVER_ARRIVING, RideStatus.RIDE_STARTED);

    private final LocationTrackingService locationTrackingService;
    private final RideRepository rideRepository;

    @MessageMapping("/driver/location")
    public void handleDriverLocationUpdate(@Payload DriverLocationUpdate update, Principal principal) {
        // Only the driver assigned to an active ride may publish its location
        Ride ride = rideRepository.findById(update.getRideId()).orElse(null);
        if (ride == null
                || !principal.getName().equals(ride.getDriverId())
                || !TRACKABLE_STATUSES.contains(ride.getStatus())) {
            log.warn("Rejected location update for ride {} from {}", update.getRideId(), principal.getName());
            return;
        }

        // Never trust the driverId in the payload
        update.setDriverId(principal.getName());

        log.info("Received driver location update for ride {}: lat={}, lng={}",
            update.getRideId(), update.getLatitude(), update.getLongitude());

        locationTrackingService.broadcastDriverLocation(update);
    }
}
