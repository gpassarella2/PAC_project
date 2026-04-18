package com.optitour.backend.security;

import com.optitour.backend.Security.JwtAuthenticationFilter;
import com.optitour.backend.Security.JwtTokenProvider;
import com.optitour.backend.repository.RevokedTokenRepository;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;

    private JwtTokenProvider tokenProvider;
    private UserDetailsService userDetailsService;
    private RevokedTokenRepository revokedTokenRepository;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setup() {
        // Mock delle dipendenze del filtro
        tokenProvider = mock(JwtTokenProvider.class);
        userDetailsService = mock(UserDetailsService.class);
        revokedTokenRepository = mock(RevokedTokenRepository.class);

        // Istanza reale del filtro con dipendenze mockate
        filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService, revokedTokenRepository);

        // Mock degli oggetti HTTP
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();

        // Pulizia del SecurityContext prima di ogni test
        SecurityContextHolder.clearContext();
    }

    // TOKEN VALIDO -> AUTENTICAZIONE IMPOSTATA -------------------

    @Test
    void validToken_setsAuthentication() throws Exception {
        String token = "valid.jwt.token";

        // Simula header Authorization: Bearer <token>
        request.addHeader("Authorization", "Bearer " + token);

        // Il token non è revocato
        when(revokedTokenRepository.existsByToken(token)).thenReturn(false);

        // Il token contiene username "gigi"
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("gigi");

        // UserDetails caricato dal database
        var userDetails = User.withUsername("gigi")
                .password("pwd")
                .authorities("USER")
                .build();

        when(userDetailsService.loadUserByUsername("gigi")).thenReturn(userDetails);

        // Il token è valido
        when(tokenProvider.validateToken(token, userDetails)).thenReturn(true);

        // Esegue il filtro
        filter.doFilter(request, response, filterChain);

        // Verifica che l'autenticazione sia stata impostata
        var auth = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(auth);
        assertEquals("gigi", auth.getName());
        assertTrue(auth instanceof UsernamePasswordAuthenticationToken);
    }

    // TOKEN REVOCATO -> NON AUTENTICA -------------------

    @Test
    void revokedToken_doesNotAuthenticate() throws Exception {
        String token = "revoked.token";

        request.addHeader("Authorization", "Bearer " + token);

        // Il token è nella blacklist -> deve essere ignorato
        when(revokedTokenRepository.existsByToken(token)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        // Nessuna autenticazione deve essere impostata
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // TOKEN GENERATO MALE -> NON PERMETTE L'AUTENTICAZIONE  -------------------

    @Test
    void badlyGeneratedToken_doesNotAuthenticate() throws Exception {
        String token = "bad.token";

        request.addHeader("Authorization", "Bearer " + token);

        when(revokedTokenRepository.existsByToken(token)).thenReturn(false);

        // Simula un'eccezione durante il parsing del token
        when(tokenProvider.getUsernameFromToken(token))
                .thenThrow(new RuntimeException("Invalid token"));

        filter.doFilter(request, response, filterChain);

        // Nessuna autenticazione deve essere impostata
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // NESSUN TOKEN -> NON AUTENTICA   -------------------

    @Test
    void noToken_doesNotAuthenticate() throws Exception {
        // Nessun header Authorization

        filter.doFilter(request, response, filterChain);

        // SecurityContext deve rimanere vuoto
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ENDPOINT /api/auth/. -> FILTRO BYPASSATO

    @Test
    void authEndpoint_isBypassed() throws Exception {
        // Gli endpoint di autenticazione non devono essere filtrati
        request.setServletPath("/api/auth/login");

        filter.doFilter(request, response, filterChain);

        // Nessuna autenticazione deve essere impostata
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // Il token provider non deve essere chiamato
        verifyNoInteractions(tokenProvider);
    }
}
