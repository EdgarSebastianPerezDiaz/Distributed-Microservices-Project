package com.distribuidos.usuario_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar información de usuario
 * 
 * @author Dev1 - Infraestructura
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    @Email(message = "Email no válido")
    private String email;

    @Size(min = 2, max = 255, message = "Nombre completo debe tener entre 2 y 255 caracteres")
    private String fullName;
    
    private String role;
}
