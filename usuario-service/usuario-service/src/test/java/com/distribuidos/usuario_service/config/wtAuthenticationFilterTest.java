package com.distribuidos.usuario_service.config;

import com.distribuidos.usuario_service.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para JwtAuthenticationFilter.
 *
 * Este test verifica que el filtro:
 * - Ignore rutas públicas
 * - Autentique correctamente cuando el token es válido
 * - No autentique cuando el token es inválido
 */
class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        // Se crea un mock del servicio JWT para simular su comportamiento
        jwtService = mock(JwtService.class);

        // Se inyecta el mock en el filtro
        filter = new JwtAuthenticationFilter(jwtService);

        // Se limpia el contexto de seguridad antes de cada prueba
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipPublicPaths() throws Exception {
        // ==============================
        // PROBANDO:
        // Que el filtro NO se aplique a rutas públicas como /api/auth/login
        // ==============================

        HttpServletRequest request = mock(HttpServletRequest.class);

        // Simulamos una petición a ruta pública
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        // Ejecutamos el método
        boolean result = filter.shouldNotFilter(request);

        // Verificamos que el filtro se omite
        assert(result);
    }

    @Test
    void shouldSetAuthenticationWhenTokenIsValid() throws Exception {
        // ==============================
        // PROBANDO:
        // Que cuando el token es válido:
        // - Se valida correctamente
        // - Se extraen los datos
        // - Se establece autenticación en Spring Security
        // ==============================

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        String token = "valid-token";

        // Simulamos request protegida
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // Simulamos comportamiento del JWT
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(UUID.randomUUID());
        when(jwtService.extractRole(token)).thenReturn("ADMINISTRADOR");

        // Ejecutamos filtro
        filter.doFilterInternal(request, response, chain);

        // Verificamos que se creó autenticación
        assert(SecurityContextHolder.getContext().getAuthentication() != null);
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenTokenIsInvalid() throws Exception {
        // ==============================
        // PROBANDO:
        // Que cuando el token es inválido:
        // - NO se genera autenticación
        // - El sistema NO falla
        // ==============================

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        String token = "invalid-token";

        // Simulamos request protegida
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // Simulamos token inválido
        when(jwtService.validateToken(token)).thenReturn(false);

        // Ejecutamos filtro
        filter.doFilterInternal(request, response, chain);

        // Verificamos que NO hay autenticación
        assert(SecurityContextHolder.getContext().getAuthentication() == null);
    }
}