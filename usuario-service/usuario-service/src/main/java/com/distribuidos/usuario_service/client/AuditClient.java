package com.distribuidos.usuario_service.client;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.distribuidos.usuario_service.dto.AuditEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditClient {
    @Value("${audit.service.url:http://localhost:8000}")
    private String auditServiceUrl;

    private final RestTemplate restTemplate;

    public void registrarEvento(AuditEventDTO evento, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<AuditEventDTO> entity = new HttpEntity<>(evento, headers);
            restTemplate.postForObject(auditServiceUrl + "/eventos", entity, Void.class);
            log.info("Evento auditoría usuario registrado: {}", evento.getTipo_evento());

        } catch (HttpClientErrorException e) {
            log.error("Error 422 desde audit-service. Body: {}", e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error registrando evento en auditoría (usuario): ", e);
        }
    }
}
