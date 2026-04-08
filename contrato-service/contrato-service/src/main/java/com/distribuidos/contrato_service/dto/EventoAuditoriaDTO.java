package com.distribuidos.contrato_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para registrar eventos de auditoría
 * FIX HC-3: Implementar integración con audit-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoAuditoriaDTO {
    
    private UUID contrato_id;
    private String tipo_evento;  // Ej: "CAMBIAR_ESTADO"
    private String estado_anterior;
    private String estado_nuevo;
    private String motivo;
    private String usuario_id;
    private String usuario_nombre;
    private String rol_usuario;
    private LocalDateTime fecha;
}
