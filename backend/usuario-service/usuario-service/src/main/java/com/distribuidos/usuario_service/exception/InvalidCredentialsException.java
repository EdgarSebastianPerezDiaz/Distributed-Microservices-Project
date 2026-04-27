package com.distribuidos.usuario_service.exception;

/**
 * Excepción lanzada cuando las credenciales de autenticación son inválidas
 * 
 * @author Dev1 - Infraestructura
 * @version 1.0
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
