package com.distribuidos.usuario_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
    
    public UserRequest() {}
    
    public UserRequest(String username, String password, String email, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}