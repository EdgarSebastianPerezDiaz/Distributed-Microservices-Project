package com.distribuidos.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    // ⚠️ EXACTAMENTE LA MISMA CLAVE
    private static final String SECRET_KEY = "mi-clave-super-secreta-para-jwt-de-512-bits-minimo-requerido-para-firmar-tokens-seguros-en-el-sistema-de-contratos-uptc-2026";

    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    // ==============================
    // VALIDAR TOKEN COMPLETO
    // ==============================
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return !isTokenExpired(token);

        } catch (Exception e) {
            System.out.println("Token inválido: " + e.getMessage());
            return false;
        }
    }

    // ==============================
    // EXTRAER CLAIMS
    // ==============================
    public static Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ==============================
    // EXTRAER ROL
    // ==============================
    public static String getRole(String token) {
        return (String) extractClaims(token).get("role");
    }

    // ==============================
    // EXTRAER USER ID
    // ==============================
    public static String getUserId(String token) {
        return extractClaims(token).getSubject();
    }

    // ==============================
    // VALIDAR EXPIRACIÓN
    // ==============================
    public static boolean isTokenExpired(String token) {
        Date expiration = extractClaims(token).getExpiration();
        return expiration.before(new Date());
    }
}