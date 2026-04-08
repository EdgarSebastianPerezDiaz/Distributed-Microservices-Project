package com.distribuidos.proveedor_service.dto;

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
public class AuditEventDTO {
    private UUID id;
    private String entityType;      // "PROVEEDOR"
    private String operationType;   // "CREATE", "UPDATE"
    private String entityId;
    private String description;
    private LocalDateTime timestamp;
    private String userRole;
    private String userId;
    private Integer version;
}