package com.distribuidos.usuario_service.config;

import com.distribuidos.usuario_service.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.distribuidos.usuario_service.security.SecurityUtils;

/**
 * Configuración de seguridad del microservicio de usuarios.
 *
 * <p>Esta clase define las reglas de autenticación y autorización del sistema,
 * así como la integración del filtro JWT en la cadena de seguridad de Spring.</p>
 *
 * <h2>Responsabilidades:</h2>
 * <ul>
 *     <li>Configurar endpoints públicos y protegidos</li>
 *     <li>Deshabilitar CSRF (API REST sin estado)</li>
 *     <li>Configurar manejo de sesiones como STATELESS</li>
 *     <li>Registrar el filtro JWT personalizado</li>
 *     <li>Definir el codificador de contraseñas (SHA-512)</li>
 * </ul>
 *
 * <h2>Seguridad implementada:</h2>
 * <ul>
 *     <li>Autenticación basada en JWT</li>
 *     <li>Autorización por roles usando @PreAuthorize</li>
 *     <li>Sin sesiones HTTP (stateless)</li>
 * </ul>
 *
 * <h2>Rutas:</h2>
 * <ul>
 *     <li>Públicas:
 *         <ul>
 *             <li>POST /api/auth/login</li>
 *             <li>/api/auth/**</li>
 *         </ul>
 *     </li>
 *     <li>Protegidas:
 *         <ul>
 *             <li>Todas las demás requieren autenticación</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author Dev1 - servicio de usuario - Lina Ladino
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private final JwtService jwtService;

    /**
     * Constructor para inyección de dependencias.
     *
     * @param jwtService servicio de manejo de tokens JWT
     */
    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Registra el filtro JWT personalizado.
     *
     * @return instancia del filtro de autenticación JWT
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService);
    }

    /**
     * Configura la cadena de filtros de seguridad.
     *
     * @param http configuración HTTP de Spring Security
     * @return SecurityFilterChain configurado
     * @throws Exception en caso de error de configuración
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF porque es una API REST sin sesiones
            .csrf(csrf -> csrf.disable())

            // Configurar sesiones como stateless (sin estado)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configurar autorización de endpoints
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )

            // Agregar filtro JWT antes del filtro de autenticación estándar
            .addFilterBefore(jwtAuthenticationFilter(), 
                           UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Define el codificador de contraseñas basado en SHA-512.
     *
     * <p>Se implementa manualmente debido a requerimientos del proyecto
     * (no uso de BCrypt).</p>
     *
     * @return PasswordEncoder personalizado
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {

            @Override
            public String encode(CharSequence rawPassword) {
                return SecurityUtils.hashSHA512(rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return SecurityUtils.verifyPassword(rawPassword.toString(), encodedPassword);
            }
        };
    }
}