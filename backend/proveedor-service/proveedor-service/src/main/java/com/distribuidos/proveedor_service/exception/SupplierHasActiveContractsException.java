package com.distribuidos.proveedor_service.exception;

import lombok.Getter;

@Getter
public class SupplierHasActiveContractsException extends RuntimeException {
    
    private final String codigo;
    
    public SupplierHasActiveContractsException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}