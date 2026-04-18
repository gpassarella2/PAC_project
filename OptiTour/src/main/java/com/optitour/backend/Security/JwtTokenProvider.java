package com.optitour.backend.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Provides utilities for generating and validating JWT tokens.
 * Uses HMAC-SHA256 (HS256) with a secret key configured in application.properties.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${OptiTour.jwt.secret}")      // Loaded from application.properties
    private String jwtSecret;

    @Value("${OptiTour.jwt.expiration}")  // Token validity duration in milliseconds
    private long jwtExpirationMs;

    /**
     * Builds the signing key used for HS256 token generation and validation.
     * The secret is used as raw bytes (not Base64-encoded).
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generates a signed JWT token for the given authenticated user.
     *
     * @param userDetails the authenticated user
     * @return a signed JWT string
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /** Extracts the username (subject) from a JWT token. */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates the token by checking:
     * - signature validity
     * - username match
     * - expiration
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    /** Returns true if the token is expired. */
    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    /** Parses and validates the JWT, returning its claims. */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
