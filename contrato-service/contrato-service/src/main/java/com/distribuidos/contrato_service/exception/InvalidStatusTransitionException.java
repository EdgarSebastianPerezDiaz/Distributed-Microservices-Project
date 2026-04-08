package com.distribuidos.contrato_service.exception;


import lombok.Getter;

@Getter
public class InvalidStatusTransitionException extends RuntimeException {
    
    private final String codigo;
    
    public InvalidStatusTransitionException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}