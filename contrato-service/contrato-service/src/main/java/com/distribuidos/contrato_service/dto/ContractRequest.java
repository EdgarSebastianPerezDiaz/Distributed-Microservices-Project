package com.distribuidos.contrato_service.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRequest {

    @NotNull(message = "Supplier ID is required")
    private UUID supplierId;

    @NotBlank(message = "Contract object is required")
    @Size(min = 200, message = "Contract object must be at least 200 characters")  // ← CAMBIO: min=200
    private String object;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal budget;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}