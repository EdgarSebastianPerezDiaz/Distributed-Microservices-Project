package com.distribuidos.proveedor_service.exception;

import lombok.Getter;

@Getter
public class SupplierNotFoundException extends RuntimeException {
    
    private final String codigo;
    
    public SupplierNotFoundException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}