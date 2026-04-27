package com.distribuidos.contrato_service.exception;


import lombok.Getter;

@Getter
public class DuplicateContractNumberException extends RuntimeException {
    
    private final String codigo;
    
    public DuplicateContractNumberException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}
