package com.distribuidos.contrato_service.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractUpdateRequest {
    
    @Size(min = 10, message = "Contract object must be at least 10 characters")
    private String object;
    
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal budget;
}