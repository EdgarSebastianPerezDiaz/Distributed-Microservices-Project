package com.distribuidos.contrato_service.client;

import com.distribuidos.contrato_service.dto.EventoAuditoriaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente para enviar eventos a audit-service
 * FIX HC-3: Implementar integración con audit-service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditClient {
    
    @Value("${audit.service.url:http://localhost:8000}")
    private String auditServiceUrl;
    
    private final RestTemplate restTemplate;
    
    /**
     * Registra un evento en el servicio de auditoría
     * Usa Eureka para resolver el nombre del servicio en producción
     */
    public void registrarEvento(EventoAuditoriaDTO evento) {
        try {
            String url = auditServiceUrl + "/eventos";
            restTemplate.postForObject(url, evento, Void.class);
            log.info("Evento registrado en auditoría: {}", evento.getTipo_evento());
        } catch (Exception e) {
            // No fallar la transacción principal si la auditoría falla
            log.error("Error registrando evento en auditoría: ", e);
        }
    }
}
