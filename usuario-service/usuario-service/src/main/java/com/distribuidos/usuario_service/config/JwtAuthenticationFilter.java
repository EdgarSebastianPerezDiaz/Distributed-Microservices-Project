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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de autenticación basado en JWT.
 *
 * <p>Este filtro intercepta todas las peticiones HTTP entrantes y se encarga de:
 * validar el token JWT enviado en el header Authorization y establecer el contexto
 * de seguridad de Spring.</p>
 *
 * <h2>Flujo de funcionamiento:</h2>
 * <ol>
 *     <li>Intercepta la petición HTTP</li>
 *     <li>Verifica si la ruta es pública (login o actuator)</li>
 *     <li>Extrae el token del header Authorization (Bearer)</li>
 *     <li>Valida el token usando {@link JwtService}</li>
 *     <li>Extrae userId y rol del token</li>
 *     <li>Crea una autenticación en el contexto de Spring Security</li>
 * </ol>
 *
 * <h2>Formato esperado del header:</h2>
 * <pre>
 * Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
 * </pre>
 *
 * <h2>Notas importantes:</h2>
 * <ul>
 *     <li>No bloquea la petición si el token es inválido, simplemente no autentica</li>
 *     <li>Las rutas públicas no pasan por validación JWT</li>
 *     <li>El rol se convierte a formato Spring: ROLE_ADMINISTRADOR</li>
 * </ul>
 *
 * @author Dev1 - servicio de usuario - Lina Ladino
 * @version 1.0
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;

    /**
     * Determina si la petición debe saltarse el filtro.
     *
     * @param request petición HTTP
     * @return true si la ruta es pública
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || 
               path.equals("/api/auth/login");
    }

    /**
     * Lógica principal del filtro.
     *
     * @param request petición HTTP
     * @param response respuesta HTTP
     * @param filterChain cadena de filtros
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");

        // Si no hay token o no es Bearer → continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Validar token
            if (jwtService.validateToken(token)) {
                String userId = jwtService.extractUserId(token).toString();
                String role = jwtService.extractRole(token);

                // Crear autoridad con formato Spring Security
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());

                // Crear autenticación
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));

                // Establecer en contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // En caso de error, limpiar contexto
            SecurityContextHolder.clearContext();
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}