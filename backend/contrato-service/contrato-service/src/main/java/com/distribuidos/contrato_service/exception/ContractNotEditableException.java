package com.distribuidos.contrato_service.exception;


import lombok.Getter;

@Getter
public class ContractNotEditableException extends RuntimeException {
    
    private final String codigo;
    
    public ContractNotEditableException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}