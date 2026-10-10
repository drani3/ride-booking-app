package com.rideapp.rideservice.security;

import com.rideapp.rideservice.model.Role;
import com.rideapp.rideservice.model.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "ZGV2LW9ubHktc2VjcmV0LWNoYW5nZS1tZS1pbi1wcm9kdWN0aW9uLTMyYnl0ZXMhIQ==";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQtdGhhdC1pcy1hbHNvLTMyLWJ5dGVzLWxvbmchISEhIQ==";

    private User user(Role role) {
        User user = new User();
        user.setId("user-123");
        user.setRole(role);
        return user;
    }

    @Test
    void tokenRoundTripsUserIdAndRole() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        Authentication auth = jwtService.toAuthentication(jwtService.generateToken(user(Role.DRIVER)));

        assertThat(auth.getPrincipal()).isEqualTo("user-123");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_DRIVER");
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, -1_000);
        String token = jwtService.generateToken(user(Role.RIDER));

        assertThatThrownBy(() -> jwtService.toAuthentication(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        String forged = new JwtService(OTHER_SECRET, 60_000).generateToken(user(Role.RIDER));

        assertThatThrownBy(() -> new JwtService(SECRET, 60_000).toAuthentication(forged))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(user(Role.RIDER));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> jwtService.toAuthentication(tampered))
                .isInstanceOf(JwtException.class);
    }
}
