package com.rideapp.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Validates the JWT once at the edge and forwards the caller's identity
 * to downstream services as X-User-Id / X-User-Role headers.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private static final String BEARER_PREFIX = "Bearer ";

    // Browsers can't send headers on the WebSocket handshake;
    // ride-service checks the token on STOMP CONNECT instead
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/**",
            "/ws-ride-tracking/**",
            "/actuator/health"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Never let clients supply identity headers themselves
        ServerHttpRequest.Builder request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                });

        String path = exchange.getRequest().getPath().value();
        if (isPublic(path)) {
            return chain.filter(exchange.mutate().request(request.build()).build());
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "Missing bearer token");
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(header.substring(BEARER_PREFIX.length()))
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT for {}: {}", path, e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }

        request.header(USER_ID_HEADER, claims.getSubject())
               .header(USER_ROLE_HEADER, claims.get("role", String.class));

        return chain.filter(exchange.mutate().request(request.build()).build());
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer body = response.bufferFactory()
                .wrap(("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(body));
    }

    // Run before routing so rejected requests never reach a service
    @Override
    public int getOrder() {
        return -1;
    }
}
