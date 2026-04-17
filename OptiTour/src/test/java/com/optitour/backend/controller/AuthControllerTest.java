package com.optitour.backend.controller;

import com.optitour.backend.dto.*;
import com.optitour.backend.model.User;

import com.optitour.backend.service.impl.AuthServiceImpl;
import com.optitour.backend.service.impl.UserServiceImpl;
import com.optitour.backend.Security.JwtTokenProvider;

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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;


import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    @Mock private UserServiceImpl userService;
    @Mock private AuthServiceImpl authService;
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
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    // ----------------------- Register -----------------------

    @Test
    void register_ShouldReturnCreatedUserProfile() throws Exception {
        Instant createdAt = Instant.parse("2024-01-01T10:00:00Z");

        // Preparazione: costruiamo il DTO che il service dovrà restituire
        // Questo simula la risposta del livello service senza toccare il DB
        UserProfileResponse response = new UserProfileResponse(
                "id123",
                "gigi",
                "gigi.rossi@mail.com",
                "Gigi",
                "Rossi",
                createdAt
        );

        // Mock: quando il controller chiama userService.register() restituiamo il DTO preparato
        when(userService.register(any())).thenReturn(response);

        // Request payload JSON che verrà inviato al controller
        // Deve corrispondere al DTO di input atteso dal controller/service
        String json = """
                {
                  "username": "gigi",
                  "email": "gigi.rossi@mail.com",
                  "password": "12345678",
                  "firstName": "Gigi",
                  "lastName": "Rossi"
                }
                """;
        // Esecuzione: inviamo la POST a /api/auth/register con il JSON
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
        		// Verifica: ci aspettiamo HTTP 201 Created
                .andExpect(status().isCreated())
                // Verifica: il body JSON contiene i campi principali corretti
                .andExpect(jsonPath("$.username").value("gigi"))
                .andExpect(jsonPath("$.email").value("gigi.rossi@mail.com"))
                .andExpect(jsonPath("$.firstName").value("Gigi"))
                .andExpect(jsonPath("$.lastName").value("Rossi"))
                // Verifica: createdAt è presente e serializzato come numero
                .andExpect(jsonPath("$.createdAt").isNumber()); // Instant serializzato come epoch
    }

    // ----------------------- Login -----------------------

    @Test
    void login_ShouldReturnJwtTokenAndUserInfo() throws Exception {
        // Crea un'istanza User per costruire il DTO di risposta.
        // Nota: il controller non usa più l'entità User direttamente, ma il DTO restituito dal service.
        User user = new User();
        user.setId("u1");
        user.setUsername("gigi");
        user.setEmail("gigi.rossi@mail.com");

        // Mock dell'autenticazione: quando il controller invoca AuthenticationManager.authenticate(...)
        // restituisce un oggetto Authentication mockato.
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Dal risultato dell'autenticazione il controller ottiene il principal.
        // authentication.getPrincipal() ritorna userDetails mockato.
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Il controller usa userDetails.getUsername() per ottenere lo username dell'utente autenticato.
        when(userDetails.getUsername()).thenReturn("gigi");

        // Mock del provider JWT: generiamo un token fisso per poterlo verificare.
        when(tokenProvider.generateToken(userDetails)).thenReturn("jwt-token-123");

        // Mock del service che restituisce il DTO del profilo utente.
        // Il controller ora chiama userService.getProfileByUsername(username) e si aspetta un DTO.
        UserProfileResponse profileDto = new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(), null, null, null
        );
        when(userService.getProfileByUsername("gigi")).thenReturn(profileDto);

        // Payload JSON della richiesta di login: deve corrispondere al DTO di input UserLoginRequest
        String json = """
                {
                  "usernameOrEmail": "gigi",
                  "password": "password"
                }
                """;

        // Esecuzione della chiamata al controller tramite MockMvc
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                // Verifica: status HTTP 200 OK
                .andExpect(status().isOk())
                // Verifica: il token restituito corrisponde al token generato dal JwtTokenProvider mockato
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                // Verifica: username nel payload di risposta corrisponde allo username del DTO
                .andExpect(jsonPath("$.username").value("gigi"))
                // Verifica: email nel payload di risposta corrisponde all'email del DTO
                .andExpect(jsonPath("$.email").value("gigi.rossi@mail.com"));
    }


    // ----------------------- Logout -----------------------

    @Test
    void logout_ShouldCallAuthServiceAndReturnMessage() throws Exception {
        // Si definisce lo username restituito dal UserDetails mockato.
        // Questo è necessario perché il controller usa currentUser.getUsername().
        when(userDetails.getUsername()).thenReturn("gigi");

        // Crea un'istanza di Authentication valida.
        // usa UsernamePasswordAuthenticationToken con il principal impostato su userDetails.
        // Le authorities vengono prese da userDetails.getAuthorities() (mockate).
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // Inserisce l'Authentication nel SecurityContext per simulare un utente autenticato.
        SecurityContextHolder.getContext().setAuthentication(auth);

        // invia la POST a /api/auth/logout con l'header Authorization contenente il token.
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer abc123"))
                // Verifica: ci aspettiamo HTTP 200 OK
                .andExpect(status().isOk())
                // Verifica: il body JSON contiene il messaggio di logout atteso
                .andExpect(jsonPath("$.message").value("Logged out successfully."));

        // Verifica comportamento: si assicura che authService.logout sia stato chiamato
        // con il token raw (senza "Bearer ") e lo username ottenuto da userDetails.
        verify(authService).logout("abc123", "gigi");

        // Pulizia: rimuoviamo l'Authentication dal SecurityContext per non contaminare altri test.
        SecurityContextHolder.clearContext();
    }


    // ----------------------- Change password -----------------------

    @Test
    void changePassword_ShouldInvokeServiceAndReturnMessage() throws Exception {
        // Si definisce lo username restituito dal UserDetails mockato.
        // Il controller usa currentUser.getUsername() per identificare l'utente autenticato.
        when(userDetails.getUsername()).thenReturn("gigi");

        // Crea un'istanza di Authentication valida.
        // usa UsernamePasswordAuthenticationToken con il principal impostato su userDetails.
        // Le authorities vengono prese da userDetails.getAuthorities() (mockate).
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // Inserisce l'Authentication nel SecurityContext per simulare un utente autenticato.
        // Questo permette al controller di risolvere @AuthenticationPrincipal durante il test.
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Payload JSON della richiesta di cambio password: deve corrispondere al DTO ChangePasswordRequest.
        String json = """
                {
                  "currentPassword": "oldpass",
                  "newPassword": "newpass123"
                }
                """;

        // Invia la POST a /api/auth/change-password con il JSON.
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                // Verifica: ci aspettiamo HTTP 200 OK
                .andExpect(status().isOk())
                // Verifica: il body JSON contiene il messaggio informativo atteso
                .andExpect(jsonPath("$.message")
                        .value("Password changed. Please log in again with your new password."));

        // Verifica comportamento: si assicura che authService.changePassword sia stato chiamato
        // con lo username ottenuto da userDetails e un oggetto ChangePasswordRequest.
        verify(authService).changePassword(eq("gigi"), any(ChangePasswordRequest.class));

        // Pulizia: rimuoviamo l'Authentication dal SecurityContext per non contaminare altri test.
        SecurityContextHolder.clearContext();
    }

}
