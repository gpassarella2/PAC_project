package com.optitour.backend.dto;

import java.time.Instant;

/** Public user profile response DTO (no password exposed). */
public class UserProfileResponse {

	// elements sent to client about the logged user

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Instant createdAt;

    public UserProfileResponse() {}

    public UserProfileResponse(String id, String username, String email, String firstName, String lastName, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
    }

    // Builder pattern implementation
    public static UserProfileResponseBuilder builder() {
        return new UserProfileResponseBuilder();
    }

    public static class UserProfileResponseBuilder {
        private String id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private Instant createdAt;

        public UserProfileResponseBuilder id(String id) { this.id = id; return this; }
        public UserProfileResponseBuilder username(String username) { this.username = username; return this; }
        public UserProfileResponseBuilder email(String email) { this.email = email; return this; }
        public UserProfileResponseBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public UserProfileResponseBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public UserProfileResponseBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public UserProfileResponse build() {
            return new UserProfileResponse(id, username, email, firstName, lastName, createdAt);
        }
    }

    //Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
