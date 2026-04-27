package com.distribuidos.contrato_service.exception;


import lombok.Getter;

@Getter
public class ContractNotFoundException extends RuntimeException {
    
    private final String codigo;
    
    public ContractNotFoundException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}