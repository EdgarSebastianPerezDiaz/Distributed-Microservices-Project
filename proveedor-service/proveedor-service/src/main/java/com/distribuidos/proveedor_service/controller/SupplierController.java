package com.distribuidos.proveedor_service.controller;

import com.distribuidos.proveedor_service.dto.SupplierRequest;
import com.distribuidos.proveedor_service.dto.SupplierResponse;
import com.distribuidos.proveedor_service.dto.SupplierUpdateRequest;
import com.distribuidos.proveedor_service.model.PersonType;
import com.distribuidos.proveedor_service.model.SupplierStatus;
import com.distribuidos.proveedor_service.service.SupplierService;
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
import com.distribuidos.proveedor_service.security.JwtPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest request) {
        log.info("POST /api/suppliers - Creating supplier");

        SupplierResponse response = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualizar proveedor existente (CON AUDITORÍA)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SupplierResponse> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierUpdateRequest request) {

        log.info("PUT /api/suppliers/{} - Updating supplier", id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        String userId = principal.getUserId();
        String userRole = principal.getRole();

        SupplierResponse response = supplierService.updateSupplier(id, request, userId, userRole);
        return ResponseEntity.ok(response);
    }

    /**
     * Cambiar estado de proveedor (ACTIVO/INACTIVO) (CON AUDITORÍA)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SupplierResponse> changeSupplierStatus(
            @PathVariable UUID id,
            @RequestParam SupplierStatus status) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        String userId = principal.getUserId();
        String userRole = principal.getRole();

        log.info("PATCH /api/suppliers/{}/status - Changing status to: {} by user: {}", id, status, userId);

        SupplierResponse response = supplierService.changeSupplierStatus(id, status, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO', 'AUDITOR')")
    public ResponseEntity<Page<SupplierResponse>> listSuppliers(
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(required = false) PersonType personType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "businessName", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("GET /api/suppliers - Listing suppliers with filters");

        Page<SupplierResponse> suppliers = supplierService.listSuppliers(status, personType, search, pageable);
        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO', 'AUDITOR')")
    public ResponseEntity<SupplierResponse> getSupplierById(@PathVariable UUID id) {
        log.info("GET /api/suppliers/{} - Fetching supplier by ID", id);

        SupplierResponse supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(supplier);
    }

    @GetMapping("/nit/{nit}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO', 'AUDITOR')")
    public ResponseEntity<SupplierResponse> getSupplierByNit(@PathVariable String nit) {
        log.info("GET /api/suppliers/nit/{} - Fetching supplier by NIT", nit);

        SupplierResponse supplier = supplierService.getSupplierByNit(nit);
        return ResponseEntity.ok(supplier);
    }

    @GetMapping("/{id}/active")
    public ResponseEntity<Boolean> isSupplierActive(@PathVariable UUID id) {
        log.debug("GET /api/suppliers/{}/active - Checking if supplier is active", id);

        boolean active = supplierService.isSupplierActive(id);
        return ResponseEntity.ok(active);
    }
}