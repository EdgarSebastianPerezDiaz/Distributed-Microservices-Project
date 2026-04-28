package com.distribuidos.usuario_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar datos de usuario
 * Permite actualizar: email, fullName, role
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
    
    @Email(message = "Email debe ser válido")
    private String email;
    
    @Size(min = 3, max = 255, message = "Full name debe tener entre 3 y 255 caracteres")
    private String fullName;
    
    @Size(min = 3, max = 50, message = "Role debe ser válido")
    private String role;
    
    // Nota: No permitimos cambiar username ni password aquí
    // El username es inmutable
    // El password se cambia con endpoint separado
}
