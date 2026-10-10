package com.rideapp.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "ZGV2LW9ubHktc2VjcmV0LWNoYW5nZS1tZS1pbi1wcm9kdWN0aW9uLTMyYnl0ZXMhIQ==";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQtdGhhdC1pcy1hbHNvLTMyLWJ5dGVzLWxvbmchISEhIQ==";

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(SECRET);

    // Captures the exchange forwarded downstream; stays null if the filter blocked it
    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    private String token(String secret, String userId, String role, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();
    }

    private MockServerWebExchange run(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, chain).block();
        return exchange;
    }

    @Test
    void validTokenForwardsIdentityHeaders() {
        run(MockServerHttpRequest.post("/api/v1/location/drivers/update")
                .header("Authorization", "Bearer " + token(SECRET, "driver-1", "DRIVER", 60_000)));

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("driver-1");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("DRIVER");
    }

    @Test
    void spoofedIdentityHeadersAreReplacedWithTokenValues() {
        run(MockServerHttpRequest.post("/api/v1/location/drivers/update")
                .header("Authorization", "Bearer " + token(SECRET, "driver-1", "DRIVER", 60_000))
                .header("X-User-Id", "someone-else")
                .header("X-User-Role", "ADMIN"));

        assertThat(forwarded.get().getRequest().getHeaders().get("X-User-Id")).containsExactly("driver-1");
        assertThat(forwarded.get().getRequest().getHeaders().get("X-User-Role")).containsExactly("DRIVER");
    }

    @Test
    void missingTokenIsRejected() {
        MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/v1/rides/ride-1"));

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredTokenIsRejected() {
        MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/v1/rides/ride-1")
                .header("Authorization", "Bearer " + token(SECRET, "rider-1", "RIDER", -1_000)));

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenSignedWithWrongKeyIsRejected() {
        MockServerWebExchange exchange = run(MockServerHttpRequest.get("/api/v1/rides/ride-1")
                .header("Authorization", "Bearer " + token(OTHER_SECRET, "rider-1", "RIDER", 60_000)));

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicPathPassesWithoutTokenButSpoofedHeadersAreStripped() {
        run(MockServerHttpRequest.post("/api/v1/auth/login")
                .header("X-User-Id", "someone-else"));

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey("X-User-Id")).isFalse();
    }
}
