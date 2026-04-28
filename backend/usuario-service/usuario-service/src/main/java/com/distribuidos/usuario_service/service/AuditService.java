package com.distribuidos.usuario_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de integración con la API de auditoría
 * 
 * Registra eventos de usuario en el servicio de auditoría:
 * - Usuario creado
 * - Usuario modificado
 * - Usuario desactivado
 * - Usuario activado
 * - Login realizado
 */
@Service
@Slf4j
public class AuditService {
    
    private final RestTemplate restTemplate;
    private static final String AUDIT_API_URL = "http://localhost:8000/eventos";
    
    public AuditService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Registra un evento de usuario en el servicio de auditoría
     * 
     * Estructura del evento:
     * {
     *   "entidad": "USUARIO",
     *   "tipoEvento": "CREACION|MODIFICACION|CAMBIO_ESTADO|LOGIN",
     *   "usuarioId": "uuid-del-usuario",
     *   "detalles": "descripción del evento",
     *   "timestamp": "ISO-8601"
     * }
     */
    public void registrarEventoUsuario(String tipoEvento, UUID usuarioId, String detalles) {
        try {
            Map<String, Object> evento = new HashMap<>();
            evento.put("entidad", "USUARIO");
            evento.put("tipoEvento", tipoEvento);
            evento.put("usuarioId", usuarioId.toString());
            evento.put("detalles", detalles);
            evento.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            
            // Enviar a la API de auditoría de forma asíncrona (sin bloquear)
            try {
                restTemplate.postForObject(AUDIT_API_URL, evento, Object.class);
                log.info("Evento de auditoría registrado: {} para usuario {}", tipoEvento, usuarioId);
            } catch (RestClientException e) {
                // Log pero no lanzar excepción - el servicio no debe fallar si auditoría está caída
                log.warn("No se pudo registrar evento en auditoría: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error al registrar evento de auditoría", e);
        }
    }
    
    /**
     * Registra creación de usuario
     */
    public void registrarCreacionUsuario(UUID usuarioId, String username, String email) {
        String detalles = String.format("Usuario creado: %s (%s)", username, email);
        registrarEventoUsuario("CREACION", usuarioId, detalles);
    }
    
    /**
     * Registra modificación de usuario
     */
    public void registrarModificacionUsuario(UUID usuarioId, String cambios) {
        String detalles = String.format("Usuario modificado: %s", cambios);
        registrarEventoUsuario("MODIFICACION", usuarioId, detalles);
    }
    
    /**
     * Registra cambio de estado de usuario
     */
    public void registrarCambioEstadoUsuario(UUID usuarioId, String nuevoEstado) {
        String detalles = String.format("Estado de usuario cambiado a: %s", nuevoEstado);
        registrarEventoUsuario("CAMBIO_ESTADO", usuarioId, detalles);
    }
    
    /**
     * Registra login de usuario
     */
    public void registrarLogin(UUID usuarioId, String username) {
        String detalles = String.format("Usuario %s inició sesión", username);
        registrarEventoUsuario("LOGIN", usuarioId, detalles);
    }
}
