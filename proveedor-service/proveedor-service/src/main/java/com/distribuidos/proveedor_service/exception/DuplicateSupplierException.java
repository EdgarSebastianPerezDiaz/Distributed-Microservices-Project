package com.distribuidos.proveedor_service.exception;

import lombok.Getter;

@Getter
public class DuplicateSupplierException extends RuntimeException {
    
    private final String codigo;
    private final String field;
    
    public DuplicateSupplierException(String codigo, String field, String message) {
        super(message);
        this.codigo = codigo;
        this.field = field;
    }
}