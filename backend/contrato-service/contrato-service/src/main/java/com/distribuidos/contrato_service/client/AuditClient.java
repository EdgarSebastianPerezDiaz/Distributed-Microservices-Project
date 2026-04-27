package com.distribuidos.contrato_service.client;

import com.distribuidos.contrato_service.dto.EventoAuditoriaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    public boolean registrarEvento(EventoAuditoriaDTO evento) {
        try {
             Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = (String) auth.getCredentials();

 HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

HttpEntity<EventoAuditoriaDTO> entity = new HttpEntity<>(evento, headers);


            String url = auditServiceUrl + "/eventos";
            restTemplate.postForObject(url, entity, Void.class);
            log.info("Evento registrado en auditoría: {}", evento.getTipo_evento());
            return true;
        } catch (Exception e) {
            // No fallar la transacción principal si la auditoría falla
            log.error("Error registrando evento en auditoría: ", e);
            return false;
        }
    }
}
