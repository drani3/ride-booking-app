package com.rideapp.rideservice.security;

import com.rideapp.rideservice.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Token subject is the user id, so every service can identify the caller
     * without a DB lookup.
     */
    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies signature and expiry, then builds a Spring Security authentication
     * with the user id as principal and ROLE_<role> as authority.
     *
     * @throws JwtException if the token is invalid or expired
     */
    public UsernamePasswordAuthenticationToken toAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String role = claims.get(ROLE_CLAIM, String.class);
        return new UsernamePasswordAuthenticationToken(
                claims.getSubject(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
