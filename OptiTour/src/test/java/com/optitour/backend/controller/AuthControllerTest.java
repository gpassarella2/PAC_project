package com.optitour.backend.controller;

import com.optitour.backend.dto.*;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.UserRepository;
import com.optitour.backend.Security.JwtTokenProvider;
import com.optitour.backend.service.AuthService;
import com.optitour.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    @Mock private UserService userService;
    @Mock private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authManager;
    @Mock private JwtTokenProvider tokenProvider;

    @Mock private Authentication authentication;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Registra il resolver per @AuthenticationPrincipal
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    // ----------------------- Register -----------------------

    @Test
    void register_ShouldReturnCreatedUserProfile() throws Exception {
        Instant createdAt = Instant.parse("2024-01-01T10:00:00Z");

        // Risposta mockata del service
        UserProfileResponse response = new UserProfileResponse(
                "id123",
                "gigi",
                "gigi.rossi@mail.com",
                "Gigi",
                "Rossi",
                createdAt
        );

        when(userService.register(any())).thenReturn(response);

        String json = """
                {
                  "username": "gigi",
                  "email": "gigi.rossi@mail.com",
                  "password": "12345678",
                  "firstName": "Gigi",
                  "lastName": "Rossi"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("gigi"))
                .andExpect(jsonPath("$.email").value("gigi.rossi@mail.com"))
                .andExpect(jsonPath("$.firstName").value("Gigi"))
                .andExpect(jsonPath("$.lastName").value("Rossi"))
                .andExpect(jsonPath("$.createdAt").isNumber()); // Instant serializzato come epoch
    }

    // ----------------------- Login -----------------------

    @Test
    void login_ShouldReturnJwtTokenAndUserInfo() throws Exception {
        // Mock dell'utente trovato nel DB
        User user = new User();
        user.setId("u1");
        user.setUsername("gigi");
        user.setEmail("gigi.rossi@mail.com");

        // Mock autenticazione
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("gigi");

        when(tokenProvider.generateToken(userDetails)).thenReturn("jwt-token-123");
        when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));

        String json = """
                {
                  "usernameOrEmail": "gigi",
                  "password": "password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.username").value("gigi"))
                .andExpect(jsonPath("$.email").value("gigi.rossi@mail.com"));
    }

    // ----------------------- Logout -----------------------

    @Test
    void logout_ShouldCallAuthServiceAndReturnMessage() throws Exception {
        when(userDetails.getUsername()).thenReturn("gigi");

        // Simula un Authentication valido 
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully."));

        verify(authService).logout("abc123", "gigi");

        SecurityContextHolder.clearContext();
    }

    // ----------------------- Change password -----------------------

    @Test
    void changePassword_ShouldInvokeServiceAndReturnMessage() throws Exception {
        when(userDetails.getUsername()).thenReturn("gigi");

        // Simula un Authentication valido
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        String json = """
                {
                  "currentPassword": "oldpass",
                  "newPassword": "newpass123"
                }
                """;

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Password changed. Please log in again with your new password."));

        verify(authService).changePassword(eq("gigi"), any(ChangePasswordRequest.class));

        SecurityContextHolder.clearContext();
    }
}
