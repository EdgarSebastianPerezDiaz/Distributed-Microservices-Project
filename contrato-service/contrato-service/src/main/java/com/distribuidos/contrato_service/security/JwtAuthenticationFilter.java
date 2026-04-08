package com.distribuidos.contrato_service.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro JWT para validar tokens en contrato-service
 * 
 * FIX HC-2: Reemplazar OAuth2 con JWT HS512 simétrico
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtils jwtUtils;
    
    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                if (jwtUtils.isTokenValid(token)) {
                    String userId = jwtUtils.extractUserId(token);
                    String username = jwtUtils.extractUsername(token);
                    String email = jwtUtils.extractEmail(token);
                    String role = jwtUtils.extractRole(token);
                    
                    // Crear principal personalizado con todos los detalles
                    JwtPrincipal principal = new JwtPrincipal(userId, username, email, role);
                    
                    // Construir la autoridad con el prefijo ROLE_
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(principal, null, 
                            Collections.singletonList(authority));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // Token inválido, continuar sin autenticación
                logger.error("Error validando JWT: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
