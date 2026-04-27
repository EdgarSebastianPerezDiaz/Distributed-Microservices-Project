package com.distribuidos.proveedor_service.dto;

import com.distribuidos.proveedor_service.model.PersonType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequest {
    
    @NotBlank(message = "NIT is required")
    private String nit;
    
    @NotBlank(message = "Business name is required")
    private String businessName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    @NotNull(message = "Person type is required")
    private PersonType personType;
}