package com.distribuidos.proveedor_service.client;

import com.distribuidos.proveedor_service.dto.AuditEventDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "servicio-auditoria", url = "${audit.service.url:http://localhost:8084}")
public interface AuditClient {
    
    @PostMapping("/api/audit/events")
    void sendAuditEvent(@RequestBody AuditEventDTO auditEvent);
}