package com.optitour.backend.controller;

import com.optitour.backend.dto.UserProfileResponse;
import com.optitour.backend.service.UserServiceIF;
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

    private final UserServiceIF userService;

    public UserController(UserServiceIF userService) {
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
     * Accepted fields: firstName, lastName, username, email.
     */
    @PatchMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestBody Map<String, String> updates) {

        String username = currentUser.getUsername();

        // Update firstName / lastName if present
        String firstName = updates.get("firstName");
        String lastName  = updates.get("lastName");
        if (firstName != null || lastName != null) {
            userService.updateProfile(username, firstName, lastName);
            // re-read username in case it changes below
        }

        // Update username / email if present
        String newUsername = updates.get("username");
        String newEmail    = updates.get("email");
        if ((newUsername != null && !newUsername.isBlank()) ||
            (newEmail    != null && !newEmail.isBlank())) {
            UserProfileResponse updated = userService.updateCredentials(username, newUsername, newEmail);
            return ResponseEntity.ok(updated);
        }

        return ResponseEntity.ok(userService.getProfileByUsername(
                (newUsername != null && !newUsername.isBlank()) ? newUsername : username));
    }

    /**
     * DELETE /api/user/profile
     * Permanently deletes the authenticated user's account and all their data.
     */
    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails currentUser) {
        userService.deleteUser(currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }
}
