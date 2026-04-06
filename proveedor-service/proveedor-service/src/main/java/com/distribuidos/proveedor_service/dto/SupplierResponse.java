package com.distribuidos.proveedor_service.dto;

import com.distribuidos.proveedor_service.model.PersonType;
import com.distribuidos.proveedor_service.model.SupplierStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponse {
    
    private UUID id;
    private String nit;
    private String businessName;
    private String email;
    private String phone;
    private PersonType personType;
    private SupplierStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}