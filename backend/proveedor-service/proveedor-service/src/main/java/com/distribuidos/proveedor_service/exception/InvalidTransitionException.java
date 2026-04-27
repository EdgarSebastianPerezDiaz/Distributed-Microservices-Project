package com.distribuidos.proveedor_service.exception;

import lombok.Getter;

@Getter
public class InvalidTransitionException extends RuntimeException {
    
    private final String codigo;
    
    public InvalidTransitionException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}