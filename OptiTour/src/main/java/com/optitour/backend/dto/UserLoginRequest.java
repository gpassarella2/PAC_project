package com.optitour.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Request DTO for user login. */
public class UserLoginRequest {

	// the user can login with email or username
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;

    public UserLoginRequest() {}

    public UserLoginRequest(String usernameOrEmail, String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }
    
    // Getters and Setters
    
    // getUsernameOrEmail: It’s used to read the username or email that the client sends to the server during login
    public String getUsernameOrEmail() { return usernameOrEmail; }
    // setUsernameOrEmail: Populates the field during the login
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
    // getPassword: It’s used to read the password that the client sends to the server during login
    public String getPassword() { return password; }
    // setPassword: Sets the raw password received from the client during login (never stored or returned)
    public void setPassword(String password) { this.password = password; }
}
