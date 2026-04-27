package com.distribuidos.usuario_service.dto;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDTO {
    

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
    private OffsetDateTime fecha;  
     private UUID contrato_id;  
}
