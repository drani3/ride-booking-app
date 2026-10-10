package com.rideapp.rideservice.security;

import com.rideapp.rideservice.model.Ride;
import com.rideapp.rideservice.repository.RideRepository;
import com.rideapp.rideservice.service.RideService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authenticates STOMP clients with the JWT sent in the CONNECT frame's
 * Authorization header, and only lets a ride's rider or driver subscribe
 * to that ride's topics.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern RIDE_TOPIC = Pattern.compile("^/topic/ride/([^/]+)/.+$");

    private final JwtService jwtService;
    private final RideRepository rideRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            String header = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (header == null || !header.startsWith(BEARER_PREFIX)) {
                throw new AccessDeniedException("Missing bearer token");
            }
            try {
                // The user set on CONNECT is attached to every later frame in this session
                accessor.setUser(jwtService.toAuthentication(header.substring(BEARER_PREFIX.length())));
            } catch (JwtException | IllegalArgumentException e) {
                throw new AccessDeniedException("Invalid token");
            }
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor.getUser(), accessor.getDestination());
        }

        return message;
    }

    private void authorizeSubscription(Principal user, String destination) {
        if (user == null || destination == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        Matcher matcher = RIDE_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            throw new AccessDeniedException("Unknown destination: " + destination);
        }

        Ride ride = rideRepository.findById(matcher.group(1))
                .orElseThrow(() -> new AccessDeniedException("Ride not found"));

        if (!RideService.isParticipant(ride, user.getName())) {
            throw new AccessDeniedException("You are not part of this ride");
        }
    }
}
