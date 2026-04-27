package com.distribuidos.usuario_service.exception;

/**
 * Excepción lanzada cuando se intenta crear un usuario con username o email duplicado
 * 
 * @author Dev1 - Infraestructura
 * @version 1.0
 */
public class UserAlreadyExistsException extends RuntimeException {
    
    public UserAlreadyExistsException(String message) {
        super(message);
    }
    
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
