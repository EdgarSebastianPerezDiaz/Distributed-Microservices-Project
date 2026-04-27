package com.distribuidos.contrato_service.dto;


import com.distribuidos.contrato_service.model.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {
    
    private UUID id;
    private UUID supplierId;
    private String supplierNit;
    private String supplierBusinessName;
    private String contractNumber;
    private String object;
    private BigDecimal budget;
    private LocalDate startDate;
    private LocalDate endDate;
    private ContractStatus status;
    private UUID createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}