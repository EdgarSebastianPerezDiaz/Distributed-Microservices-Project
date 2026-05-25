package com.distribuidos.proveedor_service.client;

import com.distribuidos.proveedor_service.dto.AuditEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditClient {
    
    @Value("${audit.service.url:http://localhost:8000}")
    private String auditServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public void registrarEvento(AuditEventDTO evento) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = (String) auth.getCredentials();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<AuditEventDTO> entity = new HttpEntity<>(evento, headers);

            String url = auditServiceUrl + "/eventos";
            ObjectMapper mapper = new ObjectMapper();
            try {
                log.info("Enviando evento de auditoría a {}: {}", url, mapper.writeValueAsString(evento));
            } catch (Exception jex) {
                log.debug("Evento (no serializable) enviado a {}: {}", url, evento);
            }

            ResponseEntity<Void> resp = restTemplate.postForEntity(url, entity, Void.class);
            if (resp != null) {
                log.info("Respuesta audit-service: {} {}", resp.getStatusCodeValue(), resp.getStatusCode());
            } else {
                log.warn("Respuesta nula de audit-service al enviar evento");
            }
            log.info("Evento registrado en auditoría: {}", evento.getTipo_evento());
        } catch (Exception e) {
            log.error("Error registrando evento en auditoría: {}", e.getMessage(), e);
        }
    }
}