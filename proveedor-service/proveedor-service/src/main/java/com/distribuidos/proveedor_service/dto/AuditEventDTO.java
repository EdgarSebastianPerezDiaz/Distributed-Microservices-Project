package com.distribuidos.proveedor_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDTO {
    
    private UUID proveedor_id;
   
    private String entidad_tipo;
    private String entidad_id;
    private String tipo_evento;
    private String descripcion;
    private String estado_anterior;
    private String estado_nuevo;
    private String motivo;
    private String usuario_id;
    private String usuario_nombre;
    private String rol_usuario;
    private Integer version;
    private LocalDateTime fecha;
}