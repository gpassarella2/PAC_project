package com.optitour.backend.Security;

import com.optitour.backend.repository.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter executed once per request.
 *
 * Responsibilities:
 * - Intercept every incoming HTTP request
 * - Extract the JWT from the Authorization header
 * - Check whether the token has been revoked (logout → blacklist)
 * - Validate the token (signature, expiration, integrity)
 * - Load the corresponding user from the database
 * - Populate the SecurityContext with the authenticated user
 *
 * This enables stateless authentication using JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // SLF4J logger bound to this class
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // Component responsible for generating, parsing, and validating JWTs
    private final JwtTokenProvider tokenProvider;

    // Standard Spring Security service for loading users from the database
    private final UserDetailsService userDetailsService;

    private final RevokedTokenRepository revokedTokenRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserDetailsService userDetailsService,
                                   RevokedTokenRepository revokedTokenRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /**
     * Main filter logic executed for every HTTP request.
     *
     * If a valid JWT is present:
     * - ensure it is not revoked
     * - extract the username
     * - load the corresponding user
     * - set the Authentication object in the SecurityContext
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Extract JWT from the Authorization header
        String token = extractToken(request);

        // Proceed only if a non-empty token is present
        if (StringUtils.hasText(token)) {

            // reject tokens explicitly revoked during logout
            if (revokedTokenRepository.existsByToken(token)) {
                logger.debug("Rejected revoked JWT token");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // Extract username from the token
                String username = tokenProvider.getUsernameFromToken(token);

                // Load user details from the database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Validate the token against the loaded user
                if (tokenProvider.validateToken(token, userDetails)) {

                    // Create the Authentication object for Spring Security
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    // Attach request-specific details (IP, session, etc.)
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Store the authenticated user in the SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ex) {
                logger.warn("Could not authenticate JWT: {}", ex.getMessage());
            }
        }
        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT from the Authorization header.
     * Expected format: "Bearer <token>"
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        // Validate header format and extract the token
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7); // Remove "Bearer "
        }
        return null;
    }
}
