package com.optitour.backend.service;

import com.optitour.backend.dto.UserRegisterRequest;
import com.optitour.backend.dto.UserProfileResponse;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.UserRepository;
import com.optitour.backend.service.impl.UserServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Abilita Mockito per i test JUnit 5
class UserServiceTest {

    @Mock
    private UserRepository userRepository; // Mock del repository MongoDB

    @Mock
    private PasswordEncoder passwordEncoder; // Mock dell'encoder password

    @InjectMocks
    private UserServiceImpl userService; // Il service sotto test, con le dipendenze mockate

    // ---------------------------- REGISTER  -------------------------------------

    @Test
    void register_success() {
        // Input della registrazione
        UserRegisterRequest req = new UserRegisterRequest(
                "gigi",
                "gigi@test.com",
                "Password123!",
                "Gigi",
                "Rossi"
        );

        // Simula che username ed email non siano già usati
        when(userRepository.existsByUsername("gigi")).thenReturn(false);
        when(userRepository.existsByEmail("gigi@test.com")).thenReturn(false);

        // Simula l'encoding della password
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");

        // Utente che il repository restituirà dopo il salvataggio
        User saved = User.builder()
                .id("123")
                .username("gigi")
                .email("gigi@test.com")
                .password("hashed")
                .firstName("Gigi")
                .lastName("Rossi")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(saved);

        // Esegue il metodo register da testare
        UserProfileResponse response = userService.register(req);

        // Verifica che il risultato sia corretto
        assertEquals("gigi", response.getUsername());
        assertEquals("gigi@test.com", response.getEmail());
        assertEquals("Gigi", response.getFirstName());
    }

    @Test
    void register_fails_whenUsernameExists() {
        // Simula che l'username esiste già
        UserRegisterRequest req = new UserRegisterRequest(
                "gigi",
                "gigi@test.com",
                "Password123!",
                "Gigi",
                "Rossi"
        );

        when(userRepository.existsByUsername("gigi")).thenReturn(true);

        // UserService deve lanciare l'eccezione
        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }

    @Test
    void register_fails_whenEmailExists() {
        // Simula che l'email è già stata registrata
        UserRegisterRequest req = new UserRegisterRequest(
                "gigi",
                "gigi@test.com",
                "Password123!",
                "Gigi",
                "Rossi"
        );

        when(userRepository.existsByUsername("gigi")).thenReturn(false);
        when(userRepository.existsByEmail("gigi@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }

    // ---------------------------- GET PROFILE BY USERNAME  -------------------------------------

    @Test
    void getProfileByUsername_success() {
        // Utente trovato nel DB
        User user = User.builder()
                .id("123")
                .username("gigi")
                .email("gigi@test.com")
                .build();

        when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfileByUsername("gigi");

        assertEquals("gigi", response.getUsername());
    }

    @Test
    void getProfileByUsername_notFound() {
        // Nessun utente trovato
        when(userRepository.findByUsername("gigi")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> userService.getProfileByUsername("gigi"));
    }

    //  ----------------------------GET PROFILE BY EMAIL ----------------------------

    @Test
    void getProfileByEmail_success() {
        User user = User.builder()
                .id("123")
                .username("gigi")
                .email("gigi@test.com")
                .build();

        when(userRepository.findByEmail("gigi@test.com")).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfileByEmail("gigi@test.com");

        assertEquals("gigi@test.com", response.getEmail());
    }

    //  ---------------------------- UPDATE PROFILE -------------------------------------

    @Test
    void updateProfile_success() {
        // Utente esistente
        User user = User.builder()
                .id("123")
                .username("gigi")
                .email("gigi@test.com")
                .firstName("Old")
                .lastName("Name")
                .build();

        when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));

        // Ritorna l'utente modificato
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse response = userService.updateProfile("gigi", "New", "Surname");

        assertEquals("New", response.getFirstName());
        assertEquals("Surname", response.getLastName());
    }
}
