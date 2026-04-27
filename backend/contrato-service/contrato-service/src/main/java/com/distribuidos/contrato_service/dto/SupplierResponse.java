package com.distribuidos.contrato_service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String status;  // ACTIVO o INACTIVO
}