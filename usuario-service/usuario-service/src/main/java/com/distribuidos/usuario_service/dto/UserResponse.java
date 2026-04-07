package com.distribuidos.usuario_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para mostrar datos de usuario (sin password!)
 */
@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String role;  // Solo el nombre del rol
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}