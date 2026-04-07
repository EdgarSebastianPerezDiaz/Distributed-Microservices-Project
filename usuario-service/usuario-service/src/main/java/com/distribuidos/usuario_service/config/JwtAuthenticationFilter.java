package com.distribuidos.usuario_service.config;

import com.distribuidos.usuario_service.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT - Se ejecuta en cada petición
 * 
 * Extrae el token del header Authorization,
 * lo valida y establece el usuario en el contexto de seguridad.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        // Si no hay header o no empieza con Bearer, continuar (será rechazado por Spring Security)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);  // Quitar "Bearer "
        
        try {
            if (jwtService.validateToken(token)) {
                String userId = jwtService.extractUserId(token).toString();
                String role = jwtService.extractRole(token);
                
                // Crear autoridad (ROLE_ADMINISTRADOR, etc.)
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                
                // Crear autenticación
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userId,  // Principal = userId
                        null,    // Credentials = null (ya validamos con JWT)
                        List.of(authority)
                    );
                
                // Establecer en el contexto
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Token inválido
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
}