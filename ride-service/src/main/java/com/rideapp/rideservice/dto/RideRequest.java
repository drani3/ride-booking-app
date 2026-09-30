package com.rideapp.rideservice.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideRequest {

    @NotBlank(message = "Rider Id is required")
    private String riderId;

    private Double pickupLatitude;

    private Double pickupLongitude;

    @NotBlank(message = "Pickup Address is required")
    private String pickupAddress;

    private Double dropLatitude;

    private Double dropLongitude;

    @NotBlank(message = "Drop address is required")
    private String dropAddress;
}