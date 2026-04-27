package com.distribuidos.proveedor_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSummaryDTO {
    
    private UUID id;
    private String contractNumber;
    private String status;
}
