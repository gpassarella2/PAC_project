package com.optitour.backend.service;

import com.optitour.backend.dto.ChangePasswordRequest;
import com.optitour.backend.model.RevokedToken;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.RevokedTokenRepository;
import com.optitour.backend.repository.UserRepository;
import com.optitour.backend.service.impl.AuthServiceImpl;
import com.optitour.backend.Security.JwtTokenProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Abilita Mockito per JUnit
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    // Imposta manualmente il valore del JWT expiration
    private final long jwtExpirationMs = 3600000; // 1 ora


    // ---------------------------- LOGOUT ----------------------------

    @Test
    void logout_savesRevokedToken_whenNotAlreadyRevoked() {
        String token = "abc123";
        String username = "gigi";

        // Simula che il token NON sia già stato revocato
        when(revokedTokenRepository.existsByToken(token)).thenReturn(false);

        // Forza il valore di jwtExpirationMs
        authService.setJwtExpirationMs(jwtExpirationMs);

        authService.logout(token, username);

        // Verifica che il token sia stato salvato come revocato
        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void logout_doesNothing_whenTokenAlreadyRevoked() {
        String token = "abc123";

        when(revokedTokenRepository.existsByToken(token)).thenReturn(true);

        authService.logout(token, "gigi");

        // Non deve salvare nulla
        verify(revokedTokenRepository, never()).save(any());
    }


    // ---------------------------- IS TOKEN REVOKED ----------------------------

    @Test
    void isTokenRevoked_returnsTrue_whenTokenExists() {
        when(revokedTokenRepository.existsByToken("abc")).thenReturn(true);

        assertTrue(authService.isTokenRevoked("abc"));
    }

    @Test
    void isTokenRevoked_returnsFalse_whenTokenNotExists() {
        when(revokedTokenRepository.existsByToken("abc")).thenReturn(false);

        assertFalse(authService.isTokenRevoked("abc"));
    }


    // ---------------------------- CHANGE PASSWORD ----------------------------

    @Test
    void changePassword_success() {
        String username = "gigi";

        User user = User.builder()
                .id("123")
                .username(username)
                .password("oldPassw")
                .build();

        ChangePasswordRequest req = new ChangePasswordRequest(
                "oldPassword",
                "newPassword"
        );

        // Simula che l'utente è stato trovato
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Simula che la password attuale sia corretta
        when(passwordEncoder.matches("oldPassword", "oldPassw")).thenReturn(true);

        // Simula l'encoding della nuova password
        when(passwordEncoder.encode("newPassword")).thenReturn("newPassw");

        authService.changePassword(username, req);

        // Verifica che la password sia stata aggiornata
        assertEquals("newPassw", user.getPassword());

        // Verifica che l'utente sia stato salvato
        verify(userRepository).save(user);

        // Verifica che tutti i token siano stati revocati
        verify(revokedTokenRepository).deleteByUsername(username);
    }

    @Test
    void changePassword_fails_whenUserNotFound() {
        when(userRepository.findByUsername("gigi")).thenReturn(Optional.empty());

        ChangePasswordRequest req = new ChangePasswordRequest("old", "new");

        assertThrows(UsernameNotFoundException.class,
                () -> authService.changePassword("gigi", req));
    }

    @Test
    void changePassword_fails_whenCurrentPasswordWrong() {
        User user = User.builder()
                .username("gigi")
                .password("oldHash")
                .build();

        ChangePasswordRequest req = new ChangePasswordRequest("wrongPassword", "newPassword");

        when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));

        // Simula che la password attuale NON è corretta
        when(passwordEncoder.matches("wrongPassword", "oldHash")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword("gigi", req));

        // Non deve salvare nulla
        verify(userRepository, never()).save(any());
    }
}
