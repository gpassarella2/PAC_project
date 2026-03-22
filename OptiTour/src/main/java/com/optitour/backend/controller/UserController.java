package com.optitour.backend.controller;

import com.optitour.backend.dto.UserProfileResponse;
import com.optitour.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User profile controller.
 * All endpoints require a valid JWT (enforced by Security config).
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/user/profile
     * Returns the authenticated user's profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(userService.getProfileByUsername(currentUser.getUsername()));
    }

    /**
     * PATCH /api/user/profile
     * Updates mutable profile fields.
     * Accepts a JSON body with optional fields: firstName, lastName.
     */
    @PatchMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestBody Map<String, String> updates) {

        UserProfileResponse updated = userService.updateProfile(
                currentUser.getUsername(),
                updates.get("firstName"),
                updates.get("lastName"));
                
        return ResponseEntity.ok(updated);
    }
}
