package com.rideapp.rideservice.controller;

import com.rideapp.rideservice.dto.RideRequest;
import com.rideapp.rideservice.dto.RideResponse;
import com.rideapp.rideservice.service.RideService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/v1/rides")

public class RideController{

    private final RideService rideService;

    public RideController(RideService rideService){
        this.rideService = rideService;
    }

    @PostMapping("/request")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<RideResponse> requestRide(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody RideRequest rideRequest){

        log.info("Ride request received from rider: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rideService.requestRide(userId, rideRequest));
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRideById(
            @AuthenticationPrincipal String userId,
            @PathVariable String rideId){
        return ResponseEntity.ok(rideService.getRideById(rideId, userId));
    }

    @GetMapping("/rider/{riderId}")
    @PreAuthorize("hasRole('RIDER') and #riderId == authentication.principal")
    public ResponseEntity<List<RideResponse>> getRidesByRider(
            @PathVariable String riderId){
        return ResponseEntity.ok(rideService.getRidesByRider(riderId));
    }

    @PutMapping("/{rideId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> startRide(
            @AuthenticationPrincipal String userId,
            @PathVariable String rideId){
        return ResponseEntity.ok(rideService.startRide(rideId, userId));
    }

    @PutMapping("/{rideId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> completeRide(
            @AuthenticationPrincipal String userId,
            @PathVariable String rideId){
        return ResponseEntity.ok(rideService.completeRide(rideId, userId));
    }

    @PutMapping("/{rideId}/cancel")
    public ResponseEntity<RideResponse> cancelRide(
            @AuthenticationPrincipal String userId,
            @PathVariable String rideId){
        return ResponseEntity.ok(rideService.cancelRide(rideId, userId));
    }

}