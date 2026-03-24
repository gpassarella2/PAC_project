package com.optitour.backend.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Objects;

/**
 * User entity – represents an authenticated user in OptiTour.
 * Stored in the "users" MongoDB collection.
 * @Document: annotation that marks the class as a MongoDB document stored in the "users" collection
 */

@Document(collection = "users")
public class User {

	// @Id: Primary key of the document
    @Id
    private String id;
    
    // @Indexed: Improves query performance and can enforce unique values
    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    /** BCrypt-hashed password – never stored in plain text. */
    private String password;

    private String firstName;
    private String lastName;

    private boolean enabled = true; // Used to deactivate an account without deleting it from DB
    
    // @CreatedDate: Automatically stores the timestamp when the document is first created.
    @CreatedDate
    private Instant createdAt;
    
    // @LastModifiedDate: Automatically stores the timestamp when the document is updated.
    @LastModifiedDate
    private Instant updatedAt;

    public User() {}

    public User(String id, String username, String email, String password, String firstName, String lastName, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Builder pattern implementation


    // We used the Builder Pattern to create User instances in a clear, fluent way,
    // especially because not all fields are required at creation time. This prevents
    // error-prone constructors with many parameters and lets us apply defaults and
    // validations in one place -> build().

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String id;
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private boolean enabled = true;
        private Instant createdAt;
        private Instant updatedAt;

        public UserBuilder id(String id) { this.id = id; return this; }
        public UserBuilder username(String username) { this.username = username; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder password(String password) { this.password = password; return this; }
        public UserBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public UserBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public UserBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public UserBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public UserBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public User build() {
            return new User(id, username, email, password, firstName, lastName, enabled, createdAt, updatedAt);
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
