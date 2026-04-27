package com.distribuidos.proveedor_service.client;


import com.distribuidos.proveedor_service.dto.ContractSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "SERVICIO-CONTRATOS", url = "${contract.service.url:http://localhost:8083}")
public interface ContractClient {
    
    @GetMapping("/api/contracts/supplier/{supplierId}/active")
    List<ContractSummaryDTO> getActiveContractsBySupplier(
            @PathVariable("supplierId") String supplierId);
}