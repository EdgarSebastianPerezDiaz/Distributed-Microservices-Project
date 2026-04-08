package com.distribuidos.contrato_service.dto;


import com.distribuidos.contrato_service.model.ContractStatus;
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
public class ContractStatusHistoryResponse {
    
    private UUID id;
    private ContractStatus previousStatus;
    private ContractStatus newStatus;
    private String reason;
    private UUID userId;
    private LocalDateTime changeDate;
}