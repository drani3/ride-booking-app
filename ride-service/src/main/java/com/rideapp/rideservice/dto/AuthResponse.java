package com.rideapp.rideservice.dto;

import com.rideapp.rideservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresInMs;
    private String userId;
    private Role role;
}
