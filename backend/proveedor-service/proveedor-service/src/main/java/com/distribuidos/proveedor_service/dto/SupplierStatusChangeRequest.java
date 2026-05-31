package com.distribuidos.proveedor_service.dto;

import com.distribuidos.proveedor_service.model.SupplierStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cambiar el estado de un proveedor
 * Acepta el nuevo estado (ACTIVO o INACTIVO)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierStatusChangeRequest {
    
    @NotNull(message = "Status is required")
    private SupplierStatus status;
}
