package com.distribuidos.usuario_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    
    @NotBlank(message = "Username es obligatorio")
    @Size(min = 3, max = 50, message = "Username debe tener entre 3 y 50 caracteres")
    private String username;
    
    @NotBlank(message = "Password es obligatorio")
    @Size(min = 6, message = "Password mínimo 6 caracteres")
    private String password;
    
    @NotBlank(message = "Email es obligatorio")
    @Email(message = "Email no válido")
    private String email;
    
    @NotBlank(message = "Nombre completo es obligatorio")
    private String fullName;
    
    @NotNull(message = "El rol es obligatorio")
    private String role;
    public void setRole(String role) { this.role = role; }
}