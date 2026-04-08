package com.distribuidos.contrato_service.controller;

import com.distribuidos.contrato_service.dto.*;
import com.distribuidos.contrato_service.model.ContractStatus;
import com.distribuidos.contrato_service.security.JwtPrincipal;
import com.distribuidos.contrato_service.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Slf4j
public class ContractController {
    
    private final ContractService contractService;
    
    @PostMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<ContractResponse> createContract(
            @Valid @RequestBody ContractRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        
        UUID userId = UUID.fromString(principal.getUserId());
        String userRole = principal.getRole();
        String userEmail = principal.getEmail();
        
        log.info("POST /api/contracts - Creating contract by user: {}", userId);
        
        ContractResponse response = contractService.createContract(request, userId, userRole, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<ContractResponse> updateContract(
            @PathVariable UUID id,
            @Valid @RequestBody ContractUpdateRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        
        UUID userId = UUID.fromString(principal.getUserId());
        String userRole = principal.getRole();
        String userEmail = principal.getEmail();
        
        log.info("PUT /api/contracts/{} - Updating contract by user: {}", id, userId);
        
        ContractResponse response = contractService.updateContract(id, request, userId, userRole, userEmail);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ContractResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusChangeRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        
        UUID userId = UUID.fromString(principal.getUserId());
        String userRole = principal.getRole();
        String userEmail = principal.getEmail();
        
        log.info("PATCH /api/contracts/{}/status - Changing status to: {} by user: {}", id, request.getNewStatus(), userId);
        
        ContractResponse response = contractService.changeStatus(id, request, userId, userRole, userEmail);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO')")
    public ResponseEntity<Void> deleteContract(
            @PathVariable UUID id) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        
        UUID userId = UUID.fromString(principal.getUserId());
        String userRole = principal.getRole();
        
        log.info("DELETE /api/contracts/{} - Deleting contract by user: {}", id, userId);
        
        contractService.deleteContract(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO', 'AUDITOR')")
    public ResponseEntity<Page<ContractResponse>> listContracts(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        
        UUID userId = UUID.fromString(principal.getUserId());
        String userRole = principal.getRole();
        
        log.info("GET /api/contracts - Listing contracts by user: {} with role: {}", userId, userRole);
        
        Page<ContractResponse> contracts = contractService.listContracts(status, search, pageable, userId, userRole);
        return ResponseEntity.ok(contracts);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO', 'AUDITOR')")
    public ResponseEntity<ContractResponse> getContractById(
            @PathVariable UUID id) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        
        UUID userId = UUID.fromString(principal.getUserId());
        String userRole = principal.getRole();
        
        log.info("GET /api/contracts/{} - Fetching contract by ID", id);
        
        ContractResponse contract = contractService.getContractById(id, userId, userRole);
        return ResponseEntity.ok(contract);
    }
    
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO', 'AUDITOR')")
    public ResponseEntity<List<ContractStatusHistoryResponse>> getContractHistory(
            @PathVariable UUID id) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        
        UUID userId = UUID.fromString(principal.getUserId());
        String userRole = principal.getRole();
        
        log.info("GET /api/contracts/{}/history - Fetching contract history", id);
        
        List<ContractStatusHistoryResponse> history = contractService.getContractHistory(id, userId, userRole);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/supplier/{supplierId}/active")
    public ResponseEntity<List<ContractSummaryDTO>> getActiveContractsBySupplier(@PathVariable UUID supplierId) {
        log.debug("GET /api/contracts/supplier/{}/active - Fetching active contracts", supplierId);
        
        List<ContractSummaryDTO> contracts = contractService.getActiveContractsBySupplier(supplierId);
        return ResponseEntity.ok(contracts);
    }
}