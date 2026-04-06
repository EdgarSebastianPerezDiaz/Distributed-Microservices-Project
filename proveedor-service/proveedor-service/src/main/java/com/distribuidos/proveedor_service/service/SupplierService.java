package com.distribuidos.proveedor_service.service;
 
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.distribuidos.proveedor_service.client.ContractClient;
import com.distribuidos.proveedor_service.dto.ContractSummaryDTO;
import com.distribuidos.proveedor_service.dto.SupplierRequest;
import com.distribuidos.proveedor_service.dto.SupplierResponse;
import com.distribuidos.proveedor_service.dto.SupplierUpdateRequest;
import com.distribuidos.proveedor_service.exception.DuplicateSupplierException;
import com.distribuidos.proveedor_service.exception.SupplierHasActiveContractsException;
import com.distribuidos.proveedor_service.exception.SupplierNotFoundException;
import com.distribuidos.proveedor_service.mapper.SupplierMapper;
import com.distribuidos.proveedor_service.model.PersonType;
import com.distribuidos.proveedor_service.model.Supplier;
import com.distribuidos.proveedor_service.model.SupplierStatus;
import com.distribuidos.proveedor_service.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SupplierService {
    
    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final ContractClient contractClient; 
    
    // Códigos de error según documento
    private static final String ERROR_NOT_FOUND = "PRV_001";
    private static final String ERROR_DUPLICATE_NIT = "PRV_002";
    private static final String ERROR_DUPLICATE_EMAIL = "PRV_002";
    private static final String ERROR_HAS_ACTIVE_CONTRACTS = "PRV_003";
    
    /**
     * Crear nuevo proveedor
     * Validar unicidad de NIT y email
     */
    public SupplierResponse createSupplier(SupplierRequest request) {
        log.info("Creating new supplier with NIT: {}", request.getNit());
        
        // Validar NIT único
        if (supplierRepository.existsByNit(request.getNit())) {
            throw new DuplicateSupplierException(ERROR_DUPLICATE_NIT, "nit", 
                "NIT already exists: " + request.getNit());
        }
        
        // Validar email único
        if (supplierRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateSupplierException(ERROR_DUPLICATE_EMAIL, "email", 
                "Email already exists: " + request.getEmail());
        }
        
        Supplier supplier = supplierMapper.toEntity(request);
        Supplier savedSupplier = supplierRepository.save(supplier);
        
        log.info("Supplier created with ID: {}", savedSupplier.getId());
        return supplierMapper.toResponse(savedSupplier);
    }
    
    /**
     * Actualizar proveedor existente
     * Validar que NIT y email no sean usados por otro proveedor
     */
    public SupplierResponse updateSupplier(UUID id, SupplierUpdateRequest request) {
        log.info("Updating supplier with ID: {}", id);
        
        Supplier existingSupplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(ERROR_NOT_FOUND, 
                    "Supplier not found with ID: " + id));
        
        // Validar email único (excluyendo el propio)
        if (request.getEmail() != null && !request.getEmail().equals(existingSupplier.getEmail())) {
            if (supplierRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new DuplicateSupplierException(ERROR_DUPLICATE_EMAIL, "email", 
                    "Email already exists: " + request.getEmail());
            }
        }
        
        supplierMapper.updateEntity(existingSupplier, request);
        Supplier updatedSupplier = supplierRepository.save(existingSupplier);
        
        log.info("Supplier updated with ID: {}", id);
        return supplierMapper.toResponse(updatedSupplier);
    }
    
    /**
     * Cambiar estado de proveedor (ACTIVO/INACTIVO)
     * Si se inactiva, validar que no tenga contratos activos (Sprint 3)
     */
    public SupplierResponse changeSupplierStatus(UUID id, SupplierStatus newStatus, String userId, String userRole) {
        log.info("Changing supplier status. ID: {}, New status: {}, User: {}", id, newStatus, userId);
        
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(ERROR_NOT_FOUND, 
                    "Supplier not found with ID: " + id));
        
        SupplierStatus oldStatus = supplier.getStatus();
        
        // Si ya está en el mismo estado, no hacer nada
        if (oldStatus == newStatus) {
            return supplierMapper.toResponse(supplier);
        }
        
        // Si se quiere desactivar (INACTIVO), validar que no tenga contratos activos
        if (newStatus == SupplierStatus.INACTIVO) {
             List<ContractSummaryDTO> activeContracts = contractClient.getActiveContractsBySupplier(id.toString());
             if (!activeContracts.isEmpty()) {
                 throw new SupplierHasActiveContractsException(ERROR_HAS_ACTIVE_CONTRACTS, 
                     "Cannot deactivate supplier with active contracts. Active contracts count: " + activeContracts.size());
             }
            log.warn("TODO: Validate no active contracts for supplier: {}", id);
        }
        
        supplier.setStatus(newStatus);
        Supplier updatedSupplier = supplierRepository.save(supplier);
        
        log.info("Supplier status changed from {} to {}", oldStatus, newStatus);
        return supplierMapper.toResponse(updatedSupplier);
    }
    
    /**
     * Listar proveedores paginado con filtros
     */
    @Transactional(readOnly = true)
    public Page<SupplierResponse> listSuppliers(SupplierStatus status, 
                                                 PersonType personType, 
                                                 String search, 
                                                 Pageable pageable) {
        log.debug("Listing suppliers with filters - status: {}, personType: {}, search: {}", 
                  status, personType, search);
        
        return supplierRepository.findAllWithFilters(status, personType, search, pageable)
                .map(supplierMapper::toResponse);
    }
    
    /**
     * Obtener proveedor por ID
     */
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(UUID id) {
        log.debug("Fetching supplier with ID: {}", id);
        
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(ERROR_NOT_FOUND, 
                    "Supplier not found with ID: " + id));
        
        return supplierMapper.toResponse(supplier);
    }
    
    /**
     * Obtener proveedor por NIT
     */
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierByNit(String nit) {
        log.debug("Fetching supplier with NIT: {}", nit);
        
        Supplier supplier = supplierRepository.findByNit(nit)
                .orElseThrow(() -> new SupplierNotFoundException(ERROR_NOT_FOUND, 
                    "Supplier not found with NIT: " + nit));
        
        return supplierMapper.toResponse(supplier);
    }
    
    /**
     * Obtener proveedor como entidad (para comunicación interna)
     */
    @Transactional(readOnly = true)
    public Supplier getSupplierEntityById(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(ERROR_NOT_FOUND, 
                    "Supplier not found with ID: " + id));
    }
    
    /**
     * Verificar si un proveedor existe y está ACTIVO
     */
    @Transactional(readOnly = true)
    public boolean isSupplierActive(UUID id) {
        return supplierRepository.findById(id)
                .map(supplier -> supplier.getStatus() == SupplierStatus.ACTIVO)
                .orElse(false);
    }
}