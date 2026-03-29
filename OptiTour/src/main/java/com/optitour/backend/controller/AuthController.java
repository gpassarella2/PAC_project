package com.optitour.backend.controller;

import com.optitour.backend.dto.UserAuthResponse;
import com.optitour.backend.dto.UserLoginRequest;
import com.optitour.backend.dto.ChangePasswordRequest;
import com.optitour.backend.dto.UserRegisterRequest;
import com.optitour.backend.dto.UserProfileResponse;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.UserRepository ;
import com.optitour.backend.Security.JwtTokenProvider;
import com.optitour.backend.service.AuthService;
import com.optitour.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService           userService;
    private final AuthService           authService;
    private final UserRepository        userRepository;
    private final AuthenticationManager authManager;
    private final JwtTokenProvider      tokenProvider;

    public AuthController(UserService userService,
                          AuthService authService,
                          UserRepository userRepository,
                          AuthenticationManager authManager,
                          JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.authManager = authManager;
        this.tokenProvider = tokenProvider;
    }

    // ------------------------- Register -------------------------

    /** POST /api/auth/register */
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(
            @Valid @RequestBody UserRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    // ------------------------- Login -------------------------

    /** POST /api/auth/login -> returns JWT bearer token */
    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(
            @Valid @RequestBody UserLoginRequest request) {

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(), request.getPassword()));

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        return ResponseEntity.ok(UserAuthResponse.builder()
                .token(token).tokenType("Bearer")
                .userId(user.getId()).username(user.getUsername()).email(user.getEmail())
                .build());
    }

    // ------------------------- Logout -------------------------

    /**
     * POST /api/auth/logout
     * Invalidates the current JWT by adding it to the revoked-token blacklist.
     * After this call, any request with the same token will receive 401.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal UserDetails currentUser) {

        String rawToken = extractToken(httpRequest);
        System.out.println("logout in corso...");
        if (StringUtils.hasText(rawToken)) {
            authService.logout(rawToken, currentUser.getUsername());
            System.out.println("logout di: " + currentUser.getUsername());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    // ------------------------- Change Password -------------------------

    /**
     * POST /api/auth/change-password
     * Verifies the current password and replaces it with a new BCrypt hash.
     * All existing JWT tokens for the user are revoked, forcing re-login.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        authService.changePassword(currentUser.getUsername(), request);
        return ResponseEntity.ok(Map.of("message",
                "Password changed. Please log in again with your new password."));
    }

    // ------------------------- utility methods -------------------------

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
