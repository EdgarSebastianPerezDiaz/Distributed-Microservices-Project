package com.distribuidos.usuario_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para solicitud de login
 * {
 *   "username": "admin",
 *   "password": "secreto123"
 * }
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "El usuario es obligatorio")
    private String username;
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}