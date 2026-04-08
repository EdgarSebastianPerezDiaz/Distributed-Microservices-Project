package com.distribuidos.contrato_service.client;


import com.distribuidos.contrato_service.dto.SupplierResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "servicio-proveedores", url = "${supplier.service.url:http://localhost:8082}")
public interface SupplierClient {
    
    @GetMapping("/api/suppliers/{id}")
    SupplierResponse getSupplierById(@PathVariable("id") UUID id);
    
    @GetMapping("/api/suppliers/{id}/active")
    Boolean isSupplierActive(@PathVariable("id") UUID id);
}