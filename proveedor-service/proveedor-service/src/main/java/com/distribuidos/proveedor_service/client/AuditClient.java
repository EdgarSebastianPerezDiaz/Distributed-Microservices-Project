package com.distribuidos.proveedor_service.client;

import com.distribuidos.proveedor_service.dto.AuditEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditClient {
    
    @Value("${audit.service.url:http://localhost:8000}")
    private String auditServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public void registrarEvento(AuditEventDTO evento) {
        try {
            String url = auditServiceUrl + "/eventos";
            restTemplate.postForObject(url, evento, Void.class);
            log.info("Evento registrado en auditoría: {}", evento.getTipo_evento());
        } catch (Exception e) {
            log.error("Error registrando evento en auditoría: ", e);
        }
    }
}