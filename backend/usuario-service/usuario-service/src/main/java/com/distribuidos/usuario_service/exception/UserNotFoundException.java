package com.distribuidos.usuario_service.exception;

/**
 * Excepción lanzada cuando no se encuentra un usuario
 * 
 * @author Dev1 - Infraestructura
 * @version 1.0
 */
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
