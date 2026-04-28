package com.distribuidos.usuario_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio para generar y validar tokens JWT
 * 
 * El token contiene:
 * - sub: ID del usuario (UUID)
 * - username: nombre de usuario
 * - role: rol del usuario (ADMINISTRADOR, etc.)
 * - iat: fecha de emisión
 * - exp: fecha de expiración (24 horas)
 */
@Service
public class JwtService {
    
    // CLAVE SECRETA: Debe ser muy larga para HS512 (mínimo 512 bits = 64 caracteres)
    // En producción, esto debe venir de variables de entorno
    private static final String SECRET_KEY = "mi-clave-super-secreta-para-jwt-de-512-bits-minimo-requerido-para-firmar-tokens-seguros-en-el-sistema-de-contratos-uptc-2026";
    
    // Duración del token: 24 horas en milisegundos
    private static final long EXPIRATION_TIME = 86400000;
    
    private final SecretKey key;
    
    public JwtService() {
        // Crear clave segura para HS512 (SHA-512)
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Genera un token JWT para un usuario
     * 
     * @param userId ID del usuario
     * @param username Username
     * @param role Rol del usuario
     * @return Token JWT firmado
     */
    public String generateToken(UUID userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("user_id", userId.toString()) 
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * Extrae todos los claims del token
     */
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Extrae el userId del token
     */
    public UUID extractUserId(String token) {
        String subject = extractClaims(token).getSubject();
        return UUID.fromString(subject);
    }
    
    /**
     * Extrae el rol del token
     */
    public String extractRole(String token) {
        return (String) extractClaims(token).get("role");
    }
    
    /**
     * Verifica si el token está expirado
     */
    public boolean isTokenExpired(String token) {
        Date expiration = extractClaims(token).getExpiration();
        return expiration.before(new Date());
    }
    
    /**
     * Valida si el token es válido (no expirado y firma correcta)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return !isTokenExpired(token);

        } catch (Exception e) {
            return false;
        }
    }
}