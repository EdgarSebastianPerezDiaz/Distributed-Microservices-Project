package com.distribuidos.proveedor_service.security;

import java.io.Serializable;

/**
 * Objeto principal personalizado que contiene información del JWT
 * Almacena los detalles del usuario decodificados del token JWT
 */
public class JwtPrincipal implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String username;
    private String email;
    private String role;
    
    public JwtPrincipal(String userId, String username, String email, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getRole() {
        return role;
    }
    
    @Override
    public String toString() {
        return username;
    }
}
