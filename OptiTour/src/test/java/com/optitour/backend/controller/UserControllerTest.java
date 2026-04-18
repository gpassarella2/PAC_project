package com.optitour.backend.controller;

import com.optitour.backend.config.GlobalExceptionHandler;
import com.optitour.backend.dto.UserProfileResponse;
import com.optitour.backend.service.impl.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    @Mock private UserServiceImpl userService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Registra il resolver necessario per @AuthenticationPrincipal
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())   // <── AGGIUNGI QUESTO
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

    }
    
    // --- helper  ------------------------------------------
    
    // imposta un utente autenticato nel SecurityContext
    private void authenticateAs(String username) {
        when(userDetails.getUsername()).thenReturn(username);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(auth);
    }

    // pulisce il SecurityContext dopo ogni test
    private void clearAuth() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    //  --- getProfile ------------------------------------------

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

    // --- updateProfile ------------------------------------------

    // PATCH /api/user/profile
    
    /**
     * Verifica che il controller aggiorni correttamente nome e cognome quando il body
     * contiene solo firstName e lastName. In questo caso:
     * - deve essere chiamato updateProfile()
     * - NON deve essere chiamato updateCredentials()
     * - la risposta deve contenere i nuovi valori aggiornati.
     */
    @Test
    void updateProfile_ShouldUpdateFirstAndLastName() throws Exception {
        authenticateAs("gigi");

        UserProfileResponse updated = new UserProfileResponse(
                "id123", "gigi", "gigi.rossi@mail.com",
                "GigiUpdated", "RossiUpdated", Instant.parse("2024-01-01T10:00:00Z"));

        when(userService.updateProfile(eq("gigi"), eq("GigiUpdated"), eq("RossiUpdated")))
                .thenReturn(updated);

        // updateCredentials NON deve essere chiamato quando non ci sono username/email nel body
        when(userService.getProfileByUsername("gigi")).thenReturn(updated);

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

        verify(userService).updateProfile("gigi", "GigiUpdated", "RossiUpdated");
        verify(userService, never()).updateCredentials(any(), any(), any());

        clearAuth();
    }
    
    /**
     * Verifica che il controller aggiorni correttamente lo username quando il body
     * contiene solo il campo "username". In questo caso:
     * - deve essere invocato updateCredentials() con il nuovo username
     * - la risposta deve contenere lo username aggiornato.
     */
    @Test
    void updateProfile_ShouldUpdateUsername() throws Exception {
        authenticateAs("gigi");

        UserProfileResponse updated = new UserProfileResponse(
                "id123", "gigi2", "gigi.rossi@mail.com",
                "Gigi", "Rossi", Instant.parse("2024-01-01T10:00:00Z"));

        when(userService.updateCredentials(eq("gigi"), eq("gigi2"), isNull()))
                .thenReturn(updated);

        String json = """
                {
                  "username": "gigi2"
                }
                """;

        mockMvc.perform(patch("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("gigi2"));

        verify(userService).updateCredentials("gigi", "gigi2", null);

        clearAuth();
    }
    
    /**
     * Verifica che il controller aggiorni correttamente l'email quando il body
     * contiene solo il campo "email". Il controller deve:
     * - chiamare updateCredentials() passando solo la nuova email
     * - restituire una risposta con l'email aggiornata.
     */
    @Test
    void updateProfile_ShouldUpdateEmail() throws Exception {
        authenticateAs("gigi");

        UserProfileResponse updated = new UserProfileResponse(
                "id123", "gigi", "nuova@mail.com",
                "Gigi", "Rossi", Instant.parse("2024-01-01T10:00:00Z"));

        when(userService.updateCredentials(eq("gigi"), isNull(), eq("nuova@mail.com")))
                .thenReturn(updated);

        String json = """
                {
                  "email": "nuova@mail.com"
                }
                """;

        mockMvc.perform(patch("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nuova@mail.com"));

        verify(userService).updateCredentials("gigi", null, "nuova@mail.com");

        clearAuth();
    }

    /**
     * Verifica che il controller gestisca correttamente l'aggiornamento
     * di username ed email quando entrambi i campi sono presenti nel body.
     * Deve chiamare updateCredentials() con entrambi i valori e restituire
     * una risposta relativa ai dati aggiornati.
     */
    @Test
    void updateProfile_ShouldUpdateUsernameAndEmail_together() throws Exception {
        authenticateAs("gigi");

        UserProfileResponse updated = new UserProfileResponse(
                "id123", "gigino", "gigino@mail.com",
                "Gigi", "Rossi", Instant.parse("2024-01-01T10:00:00Z"));

        when(userService.updateCredentials(eq("gigi"), eq("gigino"), eq("gigino@mail.com")))
                .thenReturn(updated);

        String json = """
                {
                  "username": "gigino",
                  "email": "gigino@mail.com"
                }
                """;

        mockMvc.perform(patch("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("gigino"))
                .andExpect(jsonPath("$.email").value("gigino@mail.com"));

        verify(userService).updateCredentials("gigi", "gigino", "gigino@mail.com");

        clearAuth();
    }

    /**
     * Verifica che il controller restituisca HTTP 409 Conflict quando il service
     * solleva un'IllegalArgumentException dovuta a uno username già esistente.
     * Il controller deve intercettare l'eccezione e mappare correttamente lo status.
     */
    @Test
    void updateProfile_Returns409_whenUsernameAlreadyTaken() throws Exception {
        authenticateAs("gigi");

        when(userService.updateCredentials(eq("gigi"), eq("mario"), isNull()))
                .thenThrow(new IllegalArgumentException("Username already taken: mario"));

        String json = """
                { "username": "mario" }
                """;

        mockMvc.perform(patch("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());

        clearAuth();
    }

    /**
     * Verifica che il controller restituisca HTTP 409 Conflict quando il service
     * solleva un'IllegalArgumentException per email già registrata. Il controller
     * deve convertire l'errore in una risposta 409.
     */
    @Test
    void updateProfile_Returns409_whenEmailAlreadyRegistered() throws Exception {
        authenticateAs("gigi");

        when(userService.updateCredentials(eq("gigi"), isNull(), eq("taken@mail.com")))
                .thenThrow(new IllegalArgumentException("Email already registered: taken@mail.com"));

        String json = """
                { "email": "taken@mail.com" }
                """;

        mockMvc.perform(patch("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());

        clearAuth();
    }
    
    // --- deleteUser ------------------------------------------
    
    // DELETE /api/user/profile
    
    /**
     * Verifica che la cancellazione dell'account vada a buon fine quando il service
     * non solleva eccezioni. Il controller deve restituire HTTP 204 No Content.
     */
    @Test
    void deleteAccount_ShouldReturn204_onSuccess() throws Exception {
        authenticateAs("gigi");

        // deleteUser non lancia eccezioni â†’ successo
        doNothing().when(userService).deleteUser("gigi");

        mockMvc.perform(delete("/api/user/profile"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser("gigi");

        clearAuth();
    }

    /**
     * Verifica che il controller passi al service lo username estratto dal token JWT.
     * Il test assicura che deleteUser() venga chiamato con lo username autenticato.
     */
    @Test
    void deleteAccount_ShouldCallServiceWithAuthenticatedUsername() throws Exception {
        // Verifica che il controller passi lo username estratto dal JWT, non un parametro
        authenticateAs("mario");

        doNothing().when(userService).deleteUser("mario");

        mockMvc.perform(delete("/api/user/profile"))
                .andExpect(status().isNoContent());

        // Deve usare "mario", non un valore hardcoded o null
        verify(userService).deleteUser("mario");
        verify(userService, never()).deleteUser("gigi");

        clearAuth();
    }

    /**
     * Verifica che il controller restituisca HTTP 404 Not Found quando il service
     * solleva una NoSuchElementException per utente inesistente.
     */
    @Test
    void deleteAccount_ShouldReturn404_whenUserNotFound() throws Exception {
        authenticateAs("ghost");

        doThrow(new NoSuchElementException("User not found: ghost"))
                .when(userService).deleteUser("ghost");

        mockMvc.perform(delete("/api/user/profile"))
                .andExpect(status().isNotFound());

        clearAuth();
    }

    /**
     * Garantisce che l'endpoint DELETE non restituisca 200 OK ma correttamente
     * 204 No Content in caso di eliminazione riuscita.
     */
    @Test
    void deleteAccount_ShouldNotReturn200() throws Exception {
        // Assicura che la risposta sia 204 No Content, non 200 OK
        authenticateAs("gigi");
        doNothing().when(userService).deleteUser("gigi");

        mockMvc.perform(delete("/api/user/profile"))
                .andExpect(status().is(204))
                .andExpect(status().isNoContent());

        clearAuth();
    }
    
}
