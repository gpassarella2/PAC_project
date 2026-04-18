package com.optitour.backend.security;

import com.optitour.backend.Security.CustomUserDetailsService;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    private UserRepository userRepository;
    private CustomUserDetailsService service;

    @BeforeEach
    void setup() {
        // Mock del repository MongoDB
        userRepository = mock(UserRepository.class);

        // Istanza reale del service con dipendenza mockata
        service = new CustomUserDetailsService(userRepository);
    }

    // Ricerca per USERNAME -> utente trovato --------------------------

    @Test
    void loadUserByUsername_findsUserByUsername() {
        User user = new User();
        user.setUsername("gigi");
        user.setPassword("pwd");
        user.setEmail("gigi@test.com");
        user.setEnabled(true);

        when(userRepository.findByUsername("gigi"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("gigi");

        assertEquals("gigi", details.getUsername());
        assertEquals("pwd", details.getPassword());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isEnabled());
    }

    // Ricerca per EMAIL -> username non trovato ma l'email sì --------------------------

    @Test
    void loadUserByUsername_findsUserByEmail() {
        User user = new User();
        user.setUsername("gigi");
        user.setPassword("pwd");
        user.setEmail("gigi@test.com");
        user.setEnabled(true);

        when(userRepository.findByUsername("gigi@test.com"))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail("gigi@test.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("gigi@test.com");

        assertEquals("gigi", details.getUsername());
        assertEquals("pwd", details.getPassword());
    }

    // Utente NON trovato -> eccezione --------------------------

    @Test
    void loadUserByUsername_throwsException_whenNotFound() {
        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown")
        );
    }

    // Utente DISABILITATO -> accountLocked = true --------------------------

    @Test
    void loadUserByUsername_returnsDisabledUserDetails() {
        User user = new User();
        user.setUsername("gigi");
        user.setPassword("pwd");
        user.setEmail("gigi@test.com");
        user.setEnabled(false); // utente disabilitato

        when(userRepository.findByUsername("gigi"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("gigi");

        assertFalse(details.isEnabled());
        assertFalse(details.isAccountNonLocked());
    }
}
