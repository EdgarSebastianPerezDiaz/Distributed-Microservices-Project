package com.distribuidos.usuario_service.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Respuesta estandarizada para errores
 * 
 * Proporciona un formato consistente para todas las respuestas de error
 * 
 * @author Dev1 - Infraestructura
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Timestamp del error
     */
    private LocalDateTime timestamp;
    
    /**
     * Código HTTP del error
     */
    private int status;
    
    /**
     * Código identificador del error (ej: USER_NOT_FOUND)
     */
    private String error;
    
    /**
     * Mensaje descriptivo del error
     */
    private String message;
    
    /**
     * Path del endpoint donde ocurrió el error
     */
    private String path;
    
    /**
     * Errores de validación específicos (si aplica)
     * Map<fieldName, errorMessage>
     */
    private Map<String, String> validationErrors;
}
