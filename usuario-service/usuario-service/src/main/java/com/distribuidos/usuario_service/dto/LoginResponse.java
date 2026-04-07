package com.distribuidos.usuario_service.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para respuesta de login exitoso
 * {
 *   "token": "eyJhbG...",
 *   "type": "Bearer",
 *   "user": { ... }
 * }
 */
@Data
@Builder
public class LoginResponse {
    private String token;
    private String type;  // Bearer
    private UserResponse user;
}