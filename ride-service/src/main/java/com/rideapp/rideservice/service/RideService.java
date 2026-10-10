package com.rideapp.rideservice.service;

import com.rideapp.rideservice.client.LocationServiceClient;
import com.rideapp.rideservice.dto.DirectionsRequest;
import com.rideapp.rideservice.dto.DirectionsResponse;
import com.rideapp.rideservice.dto.GeocodingRequest;
import com.rideapp.rideservice.dto.GeocodingResponse;
import com.rideapp.rideservice.dto.RouteUpdate;
import com.rideapp.rideservice.dto.RideRequest;
import com.rideapp.rideservice.dto.RideResponse;
import com.rideapp.rideservice.event.RideRequestedEvent;
import com.rideapp.rideservice.model.Ride;
import com.rideapp.rideservice.model.RideStatus;
import com.rideapp.rideservice.repository.RideRepository;
import com.rideapp.rideservice.service.LocationTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final KafkaTemplate<String, RideRequestedEvent>  kafkaTemplate;
    private final LocationServiceClient locationServiceClient;
    private final LocationTrackingService locationTrackingService;

    private static final String RIDE_REQUESTED_TOPIC = "ride.requested";

    /**
     * create ride in DB with REQUESTED STATUS
     */

    public RideResponse requestRide(String riderId, RideRequest request){
        log.info("New ride request from rider: {}", riderId);

        // Geocode addresses if coordinates not provided
        double pickupLat, pickupLng, dropLat, dropLng;
        String formattedPickupAddress = request.getPickupAddress();
        String formattedDropAddress = request.getDropAddress();

        // Handle pickup location
        if (request.getPickupLatitude() != null && request.getPickupLongitude() != null) {
            pickupLat = request.getPickupLatitude();
            pickupLng = request.getPickupLongitude();
            log.info("Using provided pickup coordinates: lat: {}, lng: {}", pickupLat, pickupLng);
        } else {
            GeocodingRequest geocodeRequest = new GeocodingRequest(request.getPickupAddress());
            GeocodingResponse pickupResponse = locationServiceClient.geocodeAddress(geocodeRequest);
            
            if (pickupResponse.isValid()) {
                pickupLat = pickupResponse.getLatitude();
                pickupLng = pickupResponse.getLongitude();
                formattedPickupAddress = pickupResponse.getFormattedAddress();
                log.info("Geocoded pickup address: {} -> lat: {}, lng: {}", request.getPickupAddress(), pickupLat, pickupLng);
            } else {
                throw new RuntimeException("Failed to geocode pickup address: " + request.getPickupAddress() + 
                    ". Error: " + pickupResponse.getErrorMessage());
            }
        }

        // Handle drop location
        if (request.getDropLatitude() != null && request.getDropLongitude() != null) {
            dropLat = request.getDropLatitude();
            dropLng = request.getDropLongitude();
            log.info("Using provided drop coordinates: lat: {}, lng: {}", dropLat, dropLng);
        } else {
            GeocodingRequest geocodeRequest = new GeocodingRequest(request.getDropAddress());
            GeocodingResponse dropResponse = locationServiceClient.geocodeAddress(geocodeRequest);
            
            if (dropResponse.isValid()) {
                dropLat = dropResponse.getLatitude();
                dropLng = dropResponse.getLongitude();
                formattedDropAddress = dropResponse.getFormattedAddress();
                log.info("Geocoded drop address: {} -> lat: {}, lng: {}", request.getDropAddress(), dropLat, dropLng);
            } else {
                throw new RuntimeException("Failed to geocode drop address: " + request.getDropAddress() + 
                    ". Error: " + dropResponse.getErrorMessage());
            }
        }

        //Step 1: save ride to database
        Ride ride = new Ride();
        ride.setRiderId(riderId);
        ride.setPickupLatitude(pickupLat);
        ride.setPickupLongitude(pickupLng);
        ride.setPickupAddress(formattedPickupAddress);
        ride.setDropLatitude(dropLat);
        ride.setDropLongitude(dropLng);
        ride.setDropAddress(formattedDropAddress);
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimatedFare(calculateEstimateFare(pickupLat, pickupLng, dropLat, dropLng));

        Ride savedRide = rideRepository.save(ride);

        // Step 2: Move to MATCHING before publishing, so a fast ride.matched
        // event can't be overwritten by this save
        savedRide.setStatus(RideStatus.MATCHING);
        savedRide = rideRepository.save(savedRide);

        // Step 3: Publish event to Kafka
        // Matching service will consume this and find nearest driver

        RideRequestedEvent event = new RideRequestedEvent(
                savedRide.getId(),
                savedRide.getRiderId(),
                savedRide.getPickupLatitude(),
                savedRide.getPickupLongitude(),
                savedRide.getPickupAddress(),
                savedRide.getDropLatitude(),
                savedRide.getDropLongitude(),
                savedRide.getDropAddress()
        );

        kafkaTemplate.send(RIDE_REQUESTED_TOPIC, savedRide.getId(), event);
        log.info("RideRequestedEvent published to Kafka for ride: {}", savedRide.getId());

        return mapToResponse(savedRide);
    }

    public void updateRideWithDriver(String rideId, String driverId){
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getStatus() != RideStatus.MATCHING) {
            log.warn("Ignoring driver {} for ride {} in status {}", driverId, rideId, ride.getStatus());
            locationServiceClient.markDriverAvailable(driverId);
            return;
        }

        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);

        // Calculate and broadcast route to rider
        calculateAndBroadcastRoute(ride);
    }

    private void calculateAndBroadcastRoute(Ride ride) {
        try {
            DirectionsRequest request = new DirectionsRequest(
                    ride.getPickupLatitude(),
                    ride.getPickupLongitude(),
                    ride.getDropLatitude(),
                    ride.getDropLongitude()
            );

            DirectionsResponse directionsResponse = locationServiceClient.calculateDirections(request);

            if (directionsResponse.isValid()) {
                // Convert to RouteUpdate DTO
                List<RouteUpdate.RouteStep> steps = new java.util.ArrayList<>();
                if (directionsResponse.getSteps() != null) {
                    for (DirectionsResponse.RouteStep step : directionsResponse.getSteps()) {
                        steps.add(new RouteUpdate.RouteStep(
                                step.getInstruction(),
                                step.getDistanceMeters(),
                                step.getDurationSeconds()
                        ));
                    }
                }

                RouteUpdate routeUpdate = new RouteUpdate(
                        ride.getId(),
                        directionsResponse.getEncodedPolyline(),
                        directionsResponse.getDistanceMeters(),
                        directionsResponse.getDurationSeconds(),
                        steps
                );

                locationTrackingService.broadcastRouteUpdate(routeUpdate);
                log.info("Route calculated and broadcast for ride {}", ride.getId());
            } else {
                log.warn("Failed to calculate route for ride {}: {}", ride.getId(), directionsResponse.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error calculating route for ride {}", ride.getId(), e);
        }
    }

    public RideResponse startRide(String rideId, String driverId){
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        requireAssignedDriver(ride, driverId);

        if(ride.getStatus() != RideStatus.ACCEPTED){
            throw new RuntimeException("Ride cannot be started. Current status: "+ride.getStatus());
        }

        ride.setStatus(RideStatus.RIDE_STARTED);
        ride.setStartedAt(LocalDateTime.now());
        rideRepository.save(ride);

        return mapToResponse(ride);
    }

    public RideResponse completeRide(String rideId, String driverId){
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        requireAssignedDriver(ride, driverId);

        if(ride.getStatus() != RideStatus.RIDE_STARTED){
            throw new RuntimeException("Ride cannot be completed. Current status: "+ride.getStatus());
        }
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride.setActualFare(ride.getEstimatedFare());
        rideRepository.save(ride);
        locationServiceClient.markDriverAvailable(ride.getDriverId());
        return mapToResponse(ride);

    }

    public RideResponse cancelRide(String rideId, String userId){
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        requireParticipant(ride, userId);

        if(ride.getStatus() == RideStatus.RIDE_STARTED
                || ride.getStatus() == RideStatus.COMPLETED
                || ride.getStatus() == RideStatus.CANCELLED){
            throw new RuntimeException("Ride cannot be cancelled. Current status: "+ride.getStatus());
        }

        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);
        if(ride.getDriverId() != null){
            locationServiceClient.markDriverAvailable(ride.getDriverId());
        }
        return mapToResponse(ride);
    }

    public RideResponse getRideById(String rideId, String userId){
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        requireParticipant(ride, userId);
        return mapToResponse(ride);
    }

    public static boolean isParticipant(Ride ride, String userId){
        return userId.equals(ride.getRiderId()) || userId.equals(ride.getDriverId());
    }

    private void requireParticipant(Ride ride, String userId){
        if(!isParticipant(ride, userId)){
            throw new AccessDeniedException("You are not part of this ride");
        }
    }

    private void requireAssignedDriver(Ride ride, String driverId){
        if(!driverId.equals(ride.getDriverId())){
            throw new AccessDeniedException("You are not the driver assigned to this ride");
        }
    }

    public List<RideResponse> getRidesByRider(String riderId){
        return rideRepository.findByRiderIdOrderByCreatedAtDesc(riderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private double calculateEstimateFare(double pickupLat, double pickupLng,
                                         double dropLat, double dropLng){
        // Simplified Haversine distance calculation
        double lat1 = Math.toRadians(pickupLat);
        double lat2 = Math.toRadians(dropLat);

        double lon1 = Math.toRadians(pickupLng);
        double lon2 = Math.toRadians(dropLng);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a =Math.pow(Math.sin(dLat / 2), 2)
                +Math.cos(lat1) * Math.cos(lat2)
                *Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double distanceKm = 6371 * c;

        //Base fare: 50Rs + 12Rs. perKm
        double fare = 50 + (distanceKm * 12);
        return Math.round(fare * 100.0) / 100.0;
    }



    private RideResponse mapToResponse(Ride ride) {
        RideResponse response = new RideResponse();
        response.setId(ride.getId());
        response.setRiderId(ride.getRiderId());
        response.setDriverId(ride.getDriverId());
        response.setPickupLatitude(ride.getPickupLatitude());
        response.setPickupLongitude(ride.getPickupLongitude());
        response.setPickupAddress(ride.getPickupAddress());
        response.setDropLatitude(ride.getDropLatitude());
        response.setDropLongitude(ride.getDropLongitude());
        response.setDropAddress(ride.getDropAddress());
        response.setStatus(ride.getStatus());
        response.setEstimatedFare(ride.getEstimatedFare());
        response.setActualFare(ride.getActualFare());
        response.setCreatedAt(ride.getCreatedAt());
        response.setStartedAt(ride.getStartedAt());
        response.setCompletedAt(ride.getCompletedAt());
        response.setUpdatedAt(ride.getUpdatedAt());
        return response;
    }
}
