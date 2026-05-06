package com.distribuidos.usuario_service.config;

import com.distribuidos.usuario_service.security.JwtService;
import com.distribuidos.usuario_service.security.DualJwtValidator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de autenticación JWT personalizado
 * 
 * Soporta dos tipos de tokens:
 * 1. JWT Legacy (HS512) - Sistema actual
 * 2. OAuth 2.0 JWT (RS256) - Nuevo sistema
 * 
 * Intenta procesar el token como JWT legacy primero.
 * Si falla, se espera que OAuth 2.0 Resource Server lo procese.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final DualJwtValidator dualJwtValidator;
    
    public JwtAuthenticationFilter(JwtService jwtService, DualJwtValidator dualJwtValidator) {
        this.jwtService = jwtService;
        this.dualJwtValidator = dualJwtValidator;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Saltar rutas públicas y OAuth
        return path.startsWith("/actuator/") || 
               path.equals("/api/auth/login") ||
               path.startsWith("/oauth2/") ||
               path.startsWith("/.well-known/");
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            // Verificar si es JWT Legacy (HS512)
            if (dualJwtValidator.isLegacyJwt(token)) {
                if (jwtService.validateToken(token)) {
                    String userId = jwtService.extractUserId(token).toString();
                    String role = jwtService.extractRole(token);
                    
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                // Si no es JWT legacy, dejar que OAuth 2.0 Resource Server lo procese
                // Simplemente continuar sin establecer autenticación
                logger.debug("Token no es JWT legacy, dejando que OAuth 2.0 Resource Server lo procese");
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            logger.debug("Token validation failed: " + e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}