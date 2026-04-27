package com.distribuidos.contrato_service.exception;



import lombok.Getter;

@Getter
public class SupplierInactiveException extends RuntimeException {
    
    private final String codigo;
    
    public SupplierInactiveException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}
