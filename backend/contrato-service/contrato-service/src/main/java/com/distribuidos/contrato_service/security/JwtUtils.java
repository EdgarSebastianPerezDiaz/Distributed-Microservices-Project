package com.distribuidos.contrato_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Utilidades para validar JWT en contrato-service
 * Usa la misma clave secreta y algoritmo que usuario-service
 * 
 * FIX HC-2: Reemplazar OAuth2 con JWT HS512 simétrico
 */
@Component
public class JwtUtils {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Extrae los claims del token
     */
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Extrae el rol (claim "role") del token
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }
    
    /**
     * Extrae el username del token
     */
    public String extractUsername(String token) {
        return extractClaims(token).get("username", String.class);
    }
    
    /**
     * Extrae el userId (subject) del token
     */
    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }
    
    /**
     * Extrae el email (claim "email") del token
     */
    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }
    
    /**
     * Valida que el token sea válido (firma correcta)
     */
    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
