package com.optitour.backend.service;

import com.optitour.backend.model.RevokedToken ;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.RevokedTokenRepository ;
import com.optitour.backend.repository.UserRepository;
import com.optitour.backend.dto.ChangePasswordRequest ;
import com.optitour.backend.Security.JwtTokenProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * AuthService – handles the full authentication lifecycle:
 * 
 *	– Login (delegated to Spring Security / AuthenticationManager)
 *  – Registration (delegated to "UserService)
 *  – Logout: invalidates the JWT by adding it to the token blacklist
 *  – Change Password: verifies current password before updating BCrypt hash
 * 
 * Stateless Logout
 * Since JWTs are stateless, a "logout" requires storing the revoked token in
 * MongoDB (the {@code revoked_tokens} collection) until the token's natural
 * expiry. The {@link com.buddymaps.security.JwtAuthenticationFilter} checks
 * this blacklist on every request.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository       userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtTokenProvider     tokenProvider;

    @Value("${buddymaps.jwt.expiration}")
    private long jwtExpirationMs;

    public AuthService(UserRepository userRepository,
                       RevokedTokenRepository revokedTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    // ───────────────────────────── Logout ────────────────────────────

    /**
     * Invalidates the supplied JWT by adding it to the revoked-token blacklist.
     * Subsequent requests using this token will be rejected by
     * {@link com.buddymaps.security.JwtAuthenticationFilter}.
     *
     * @param rawToken the raw JWT string (without "Bearer " prefix)
     * @param username the authenticated user's username
     */
    public void logout(String rawToken, String username) {
        if (revokedTokenRepository.existsByToken(rawToken)) {
            logger.debug("Token already revoked for user [{}]", username);
            return;
        }

        Instant expiresAt = Instant.now()
                .plusSeconds(jwtExpirationMs / 1000);

        RevokedToken revoked = new RevokedToken(
                null,
                rawToken,
                username,
                Instant.now(),
                expiresAt
        );

        revokedTokenRepository.save(revoked);
        logger.info("JWT revoked for user [{}]. Effective until {}", username, expiresAt);
    }

    /**
     * Returns true if the given token has been revoked (i.e. the user logged out).
     */
    public boolean isTokenRevoked(String rawToken) {
        return revokedTokenRepository.existsByToken(rawToken);
    }

    // ───────────────────────────── Change Password ──────────────────

    /**
     * Changes the user's password after verifying the current one.
     *
     * @param username         the authenticated user's username
     * @param request          DTO containing the current and new passwords
     * @throws IllegalArgumentException if the current password is wrong
     */
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing tokens for this user to force re-login
        revokedTokenRepository.deleteByUsername(username);
        logger.info("Password changed for user [{}]. All existing tokens revoked.", username);
    }
}
