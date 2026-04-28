package com.distribuidos.usuario_service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidades de Seguridad
 * 
 * Implementa hash SHA-512 para passwords como lo exige el proyecto.
 * SHA-512 produce 128 caracteres hexadecimales.
 * 
 * NOTA: NO usamos BCrypt porque el proyecto especifica SHA-512.
 */
public class SecurityUtils {
    
    /**
     * Hashea un password usando SHA-512
     * 
     * @param password Password en texto plano
     * @return Hash hexadecimal de 128 caracteres (SHA-512)
     */
    public static String hashSHA512(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Convertir a hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            // SHA-512 siempre produce 128 caracteres hex
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error: SHA-512 no disponible", e);
        }
    }
    
    /**
     * Verifica si un password coincide con un hash
     * 
     * @param password Password en texto plano
     * @param hash Hash almacenado (128 caracteres)
     * @return true si coinciden
     */
    public static boolean verifyPassword(String password, String hash) {
        String passwordHash = hashSHA512(password);
        return passwordHash.equalsIgnoreCase(hash);  // ignorar caso por si acaso
    }
}