package com.optitour.backend.service.impl;

import com.optitour.backend.dto.UserRegisterRequest;
import com.optitour.backend.dto.UserProfileResponse;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.UserRepository;
import com.optitour.backend.repository.TripRepository;
import com.optitour.backend.service.UserServiceIF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Service layer for user registration, profile management and account deletion.
 */
@Service
public class UserServiceImpl implements UserServiceIF {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           TripRepository tripRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.tripRepository  = tripRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Registers a new user and returns their profile. */
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

    /** Updates mutable profile fields (firstName, lastName). */
    public UserProfileResponse updateProfile(String username, String firstName, String lastName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        if (firstName != null) user.setFirstName(firstName);
        if (lastName  != null) user.setLastName(lastName);

        return toProfileResponse(userRepository.save(user));
    }

    /**
     * Updates username and/or email.
     * Blank/null values are ignored. Uniqueness is validated before saving.
     */
    public UserProfileResponse updateCredentials(String currentUsername,
                                                 String newUsername,
                                                 String newEmail) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + currentUsername));

        if (newUsername != null && !newUsername.isBlank() && !newUsername.equals(currentUsername)) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new IllegalArgumentException("Username already taken: " + newUsername);
            }
            user.setUsername(newUsername);
        }

        if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(user.getEmail())) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email already registered: " + newEmail);
            }
            user.setEmail(newEmail);
        }

        User saved = userRepository.save(user);
        logger.info("Updated credentials for user [{}]", saved.getUsername());
        return toProfileResponse(saved);
    }

    /**
     * Permanently deletes the user account and all associated trips from the DB.
     */
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        // Cascade: remove all trips belonging to the user
        tripRepository.deleteByUserId(user.getId());

        userRepository.delete(user);
        logger.info("Permanently deleted user [{}] and all their trips", username);
    }

    // ---------------------- mapper ----------------------

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
