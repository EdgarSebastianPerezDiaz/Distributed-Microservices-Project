package com.distribuidos.usuario_service.config;

import com.distribuidos.usuario_service.security.JwtService;
import com.distribuidos.usuario_service.security.DualJwtValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.distribuidos.usuario_service.security.SecurityUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private final JwtService jwtService;
    private final DualJwtValidator dualJwtValidator;
    
    public SecurityConfig(JwtService jwtService, DualJwtValidator dualJwtValidator) {
        this.jwtService = jwtService;
        this.dualJwtValidator = dualJwtValidator;
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, dualJwtValidator);
    }
    
    /**
     * Cadena de filtros de seguridad para API REST
     * Orden: 2 (después de AuthorizationServerConfig)
     * 
     * Soporta:
     * - JWT legacy (HS512) del sistema actual
     * - OAuth 2.0 JWT (RS256) del nuevo sistema
     * - Endpoints de OAuth 2.0
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Permitir métodos OPTIONS (CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // RUTAS PÚBLICAS
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                
                // RUTAS OAUTH 2.0 (manejadas por Authorization Server)
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/.well-known/**").permitAll()
                
                // RUTAS PROTEGIDAS - Requieren autenticación
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), 
                           UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
    
    @Bean
    @Primary
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