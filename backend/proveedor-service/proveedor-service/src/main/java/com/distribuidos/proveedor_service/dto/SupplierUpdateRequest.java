package com.distribuidos.proveedor_service.dto;


import com.distribuidos.proveedor_service.model.SupplierStatus;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierUpdateRequest {
    
    private String businessName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    
    private SupplierStatus status;
}