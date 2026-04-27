package com.distribuidos.usuario_service.exception;

/**
 * Excepción lanzada cuando se intenta usar un rol que no existe
 * 
 * @author Dev1 - Infraestructura
 * @version 1.0
 */
public class InvalidRoleException extends RuntimeException {
    
    public InvalidRoleException(String message) {
        super(message);
    }
    
    public InvalidRoleException(String message, Throwable cause) {
        super(message, cause);
    }
}
