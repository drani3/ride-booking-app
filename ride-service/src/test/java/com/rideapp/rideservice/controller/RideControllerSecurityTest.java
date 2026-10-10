package com.rideapp.rideservice.controller;

import com.rideapp.rideservice.dto.RideResponse;
import com.rideapp.rideservice.model.Role;
import com.rideapp.rideservice.model.User;
import com.rideapp.rideservice.security.JwtAuthenticationFilter;
import com.rideapp.rideservice.security.JwtService;
import com.rideapp.rideservice.security.SecurityConfig;
import com.rideapp.rideservice.service.RideService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RideController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ZGV2LW9ubHktc2VjcmV0LWNoYW5nZS1tZS1pbi1wcm9kdWN0aW9uLTMyYnl0ZXMhIQ==",
        "jwt.expiration-ms=60000"
})
class RideControllerSecurityTest {

    private static final String RIDE_BODY =
            "{\"pickupAddress\":\"A\",\"pickupLatitude\":12.9,\"pickupLongitude\":77.5," +
            "\"dropAddress\":\"B\",\"dropLatitude\":12.95,\"dropLongitude\":77.6}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private RideService rideService;

    private String bearer(String userId, Role role) {
        User user = new User();
        user.setId(userId);
        user.setRole(role);
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/rides/request")
                        .contentType(MediaType.APPLICATION_JSON).content(RIDE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithGarbageTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/rides/request")
                        .header("Authorization", "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON).content(RIDE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void riderCanRequestRideAndRiderIdComesFromToken() throws Exception {
        when(rideService.requestRide(eq("rider-1"), any())).thenReturn(new RideResponse());

        mockMvc.perform(post("/api/v1/rides/request")
                        .header("Authorization", bearer("rider-1", Role.RIDER))
                        .contentType(MediaType.APPLICATION_JSON).content(RIDE_BODY))
                .andExpect(status().isCreated());

        verify(rideService).requestRide(eq("rider-1"), any());
    }

    @Test
    void driverCannotRequestRide() throws Exception {
        mockMvc.perform(post("/api/v1/rides/request")
                        .header("Authorization", bearer("driver-1", Role.DRIVER))
                        .contentType(MediaType.APPLICATION_JSON).content(RIDE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void riderCannotListAnotherRidersRides() throws Exception {
        mockMvc.perform(get("/api/v1/rides/rider/rider-2")
                        .header("Authorization", bearer("rider-1", Role.RIDER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void riderCanListOwnRides() throws Exception {
        mockMvc.perform(get("/api/v1/rides/rider/rider-1")
                        .header("Authorization", bearer("rider-1", Role.RIDER)))
                .andExpect(status().isOk());
    }
}
