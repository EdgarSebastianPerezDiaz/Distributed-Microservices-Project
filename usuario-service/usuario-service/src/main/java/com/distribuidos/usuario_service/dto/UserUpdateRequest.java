package com.distribuidos.usuario_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UserUpdateRequest {
     @Email(message = "Email no válido")
    private String email;

       @Size(min = 2, max = 255, message = "Nombre completo debe tener entre 2 y 255 caracteres")
    private String fullName;
    private String role; 


     public UserUpdateRequest() {}

     //Cambio de gmail
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    //Cambio en el nombre no Username
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    //Cambio en el rol
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
