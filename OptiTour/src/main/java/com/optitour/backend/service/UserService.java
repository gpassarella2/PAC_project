package com.optitour.backend.service;

import com.optitour.backend.dto.UserRegisterRequest;
import com.optitour.backend.dto.UserProfileResponse;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Service layer for user registration and profile management.
 */

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user and returns their profile.
     *
     * @throws IllegalArgumentException if username or email is already taken
     */
    public UserProfileResponse register(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        User saved = userRepository.save(user);
        logger.info("Registered new user [{}]", saved.getUsername());
        return toProfileResponse(saved);
    }

    /** Returns the public profile of the user with the given username. */
    public UserProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        return toProfileResponse(user);
    }
    
    /** Returns the public profile of the user with the given email. */    
    public UserProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + email));
        return toProfileResponse(user);
    }


    /**
     * Updates mutable profile fields (firstName, lastName, homeCity).
     */
    public UserProfileResponse updateProfile(String username,
                                             String firstName,
                                             String lastName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        if (firstName != null)  user.setFirstName(firstName);
        if (lastName  != null)  user.setLastName(lastName);

        return toProfileResponse(userRepository.save(user));
    }

    // ──────────────────────────── mapper ─────────────────────────────────

    /**
     * Maps a User entity to a UserProfileResponse DTO.
     *
     * @param user the User entity to convert
     * @return the corresponding UserProfileResponse
     */

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
