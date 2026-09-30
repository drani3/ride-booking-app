package com.rideapp.rideservice.service;

import com.rideapp.rideservice.dto.DriverLocationUpdate;
import com.rideapp.rideservice.dto.ETAUpdate;
import com.rideapp.rideservice.dto.RouteUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationTrackingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisPublisher redisPublisher;
    
    private final Map<String, String> rideToRiderSession = new ConcurrentHashMap<>();
    private final Map<String, String> rideToDriverSession = new ConcurrentHashMap<>();

    public void subscribeRiderToRide(String rideId, String riderId, String sessionId) {
        rideToRiderSession.put(rideId, sessionId);
        log.info("Rider {} subscribed to ride {}", riderId, rideId);
    }

    public void unsubscribeRiderFromRide(String rideId) {
        rideToRiderSession.remove(rideId);
        log.info("Rider unsubscribed from ride {}", rideId);
    }

    public void subscribeDriverToRide(String rideId, String driverId, String sessionId) {
        rideToDriverSession.put(rideId, sessionId);
        log.info("Driver {} subscribed to ride {}", driverId, rideId);
    }

    public void unsubscribeDriverFromRide(String rideId) {
        rideToDriverSession.remove(rideId);
        log.info("Driver unsubscribed from ride {}", rideId);
    }

    public void broadcastDriverLocation(DriverLocationUpdate update) {
        String topic = "/topic/ride/" + update.getRideId() + "/driver-location";
        messagingTemplate.convertAndSend(topic, update);
        
        // Also publish to Redis for multi-instance support
        redisPublisher.publishDriverLocation(update);
        
        log.debug("Broadcasted driver location for ride {}", update.getRideId());
    }

    public void broadcastRouteUpdate(RouteUpdate update) {
        String topic = "/topic/ride/" + update.getRideId() + "/route";
        messagingTemplate.convertAndSend(topic, update);
        log.info("Broadcasted route update for ride {}", update.getRideId());
    }

    public void broadcastETAUpdate(ETAUpdate update) {
        String topic = "/topic/ride/" + update.getRideId() + "/eta";
        messagingTemplate.convertAndSend(topic, update);
        log.debug("Broadcasted ETA update for ride {}", update.getRideId());
    }
}
