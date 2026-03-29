package com.optitour.backend.controller;

import com.optitour.backend.dto.UserProfileResponse;
import com.optitour.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    @Mock private UserService userService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Registra il resolver necessario per @AuthenticationPrincipal
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    //  ---------------------------- getProfile ----------------------------

    @Test
    void getProfile_ShouldReturnUserProfile() throws Exception {
        // Simula un utente autenticato: necessario perché il controller usa @AuthenticationPrincipal
        when(userDetails.getUsername()).thenReturn("gigi");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(auth);

        // Risposta mockata del service
        UserProfileResponse response = new UserProfileResponse(
                "id123", "gigi", "gigi.rossi@mail.com",
                "Gigi", "Rossi",
                Instant.parse("2024-01-01T10:00:00Z")
        );

        when(userService.getProfileByUsername("gigi")).thenReturn(response);

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("gigi"))
                .andExpect(jsonPath("$.email").value("gigi.rossi@mail.com"))
                .andExpect(jsonPath("$.firstName").value("Gigi"))
                .andExpect(jsonPath("$.lastName").value("Rossi"))
                .andExpect(jsonPath("$.createdAt").isNumber()); // Instant serializzato come epoch

        // Pulizia del contesto di sicurezza
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    // ----------------------------updateProfile ----------------------------

    @Test
    void updateProfile_ShouldUpdateAndReturnUserProfile() throws Exception {
        // Simula l'utente autenticato
        when(userDetails.getUsername()).thenReturn("gigi");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(auth);

        // Risposta mockata del service dopo l'update
        UserProfileResponse updated = new UserProfileResponse(
                "id123", "gigi", "gigi.rossi@mail.com",
                "GigiUpdated", "RossiUpdated",
                Instant.parse("2024-01-01T10:00:00Z")
        );

        when(userService.updateProfile(eq("gigi"), eq("GigiUpdated"), eq("RossiUpdated")))
                .thenReturn(updated);

        String json = """
                {
                  "firstName": "GigiUpdated",
                  "lastName": "RossiUpdated"
                }
                """;

        mockMvc.perform(patch("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("GigiUpdated"))
                .andExpect(jsonPath("$.lastName").value("RossiUpdated"));

        // Pulizia del contesto
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
