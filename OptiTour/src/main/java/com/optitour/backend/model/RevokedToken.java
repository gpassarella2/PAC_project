package com.optitour.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * RevokedToken entity – stores invalidated JWT tokens until their natural expiry,
 * enabling a stateless logout mechanism.
 */
@Document(collection = "revoked_tokens")
public class RevokedToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    private String username;
    private Instant revokedAt;
    private Instant expiresAt;

    public RevokedToken() {}

    public RevokedToken(String id, String token, String username, Instant revokedAt, Instant expiresAt) {
        this.id = id;
        this.token = token;
        this.username = username;
        this.revokedAt = revokedAt;
        this.expiresAt = expiresAt;
    }

    //Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}

