package com.rideapp.rideservice.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideRequest {

    // riderId is taken from the JWT, never from the request body

    private Double pickupLatitude;

    private Double pickupLongitude;

    @NotBlank(message = "Pickup Address is required")
    private String pickupAddress;

    private Double dropLatitude;

    private Double dropLongitude;

    @NotBlank(message = "Drop address is required")
    private String dropAddress;
}