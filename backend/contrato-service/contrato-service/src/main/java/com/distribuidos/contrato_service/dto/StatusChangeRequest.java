package com.distribuidos.contrato_service.dto;

import com.distribuidos.contrato_service.model.ContractStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeRequest {
    
    @NotNull(message = "New status is required")
    private ContractStatus newStatus;
    
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
