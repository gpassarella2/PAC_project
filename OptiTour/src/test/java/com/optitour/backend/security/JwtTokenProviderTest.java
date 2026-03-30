package com.optitour.backend.security;

import com.optitour.backend.Security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private final String SECRET = "12345678901234567890123456789012"; // 32+ chars for HS256
    private final long EXPIRATION = 3600000L; // 1 ora per non farlo scadere durante il test

    private UserDetails userDetails;

    @BeforeEach
    void setup() {
        jwtTokenProvider = new JwtTokenProvider();

        // Imposta i campi privati tramite reflection(altrimenti non si potrebbe)
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", EXPIRATION);

        // Utente
        userDetails = User.withUsername("gigi")
                .password("pwd")
                .authorities("USER")
                .build();
    }

    // ---------------------------- GENERAZIONE TOKEN -----------------------------
    
    @Test
    void generateToken_returnsValidJwt() {
        String token = jwtTokenProvider.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    // ---------------------------- ESTRAZIONE USERNAME DAL TOKEN -----------------------------

    @Test
    void getUsernameFromToken_returnsCorrectUsername() {
        String token = jwtTokenProvider.generateToken(userDetails);

        String username = jwtTokenProvider.getUsernameFromToken(token);

        assertEquals("gigi", username);
    }

    // ---------------------------- VALIDAZIONE TOKEN -----------------------------

    @Test
    void validateToken_returnsTrue_forValidToken() {
        String token = jwtTokenProvider.generateToken(userDetails);

        assertTrue(jwtTokenProvider.validateToken(token, userDetails));
    }

    @Test
    void validateToken_returnsFalse_forDifferentUsername() {
        String token = jwtTokenProvider.generateToken(userDetails);

        UserDetails otherUser = User.withUsername("mario")
                .password("pwd")
                .authorities("USER")
                .build();

        assertFalse(jwtTokenProvider.validateToken(token, otherUser));
    }

    // ---------------------------- TOKEN SCADUTO -----------------------------

    @Test
    void validateToken_returnsFalse_forExpiredToken() {
        // Impostiamo scadenza a -1 ms -> token già scaduto
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1L);

        String token = jwtTokenProvider.generateToken(userDetails);

        assertFalse(jwtTokenProvider.validateToken(token, userDetails));
    }

    // ---------------------------- TOKEN GENERATO MALE -----------------------------

    @Test
    void validateToken_returnsFalse_forBadlyGeneratedToken() {
        String invalidToken = "malformeToken";

        assertFalse(jwtTokenProvider.validateToken(invalidToken, userDetails));
    }
}
