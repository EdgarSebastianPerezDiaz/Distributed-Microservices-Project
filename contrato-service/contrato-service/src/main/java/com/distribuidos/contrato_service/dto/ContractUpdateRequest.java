package com.distribuidos.contrato_service.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractUpdateRequest {

    // object NO es modificable según RF-CONT-02
    // Solo se puede modificar el presupuesto

    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal budget;
}