package com.distribuidos.contrato_service.service;


import com.distribuidos.contrato_service.client.AuditClient;
import com.distribuidos.contrato_service.client.SupplierClient;
import com.distribuidos.contrato_service.dto.*;
import com.distribuidos.contrato_service.exception.*;
import com.distribuidos.contrato_service.mapper.ContractMapper;
import com.distribuidos.contrato_service.model.Contract;
import com.distribuidos.contrato_service.model.ContractStatus;
import com.distribuidos.contrato_service.model.ContractStatusHistory;
import com.distribuidos.contrato_service.repository.ContractRepository;
import com.distribuidos.contrato_service.repository.ContractStatusHistoryRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContractService {
    
    private final ContractRepository contractRepository;
    private final ContractStatusHistoryRepository historyRepository;
    private final ContractMapper contractMapper;
    private final ContractStateMachine stateMachine;
    private final SupplierClient supplierClient;
    private final AuditClient auditClient;
    
    // Códigos de error según documento
    private static final String ERROR_NOT_FOUND = "CTR_001";
    private static final String ERROR_DUPLICATE_NUMBER = "CTR_002";
    private static final String ERROR_NOT_EDITABLE = "CTR_004";
    private static final String ERROR_INVALID_TRANSITION = "CTR_005";
    private static final String ERROR_SUPPLIER_NOT_FOUND = "CTR_006";
    private static final String ERROR_SUPPLIER_INACTIVE = "CTR_007";
    
    /**
     * Crear un nuevo contrato
     * Reglas: estado inicial BORRADOR, validar proveedor existente y ACTIVO
     */
    public ContractResponse createContract(ContractRequest request, UUID userId, String userRole, String userEmail) {
        log.info("Creating new contract for supplier: {} by user: {}", request.getSupplierId(), userId);
        
        // Validar fechas
        validateDates(request.getStartDate(), request.getEndDate());
        
        // Validar proveedor existe y está ACTIVO (Sprint 3)
        SupplierResponse supplier = validateSupplier(request.getSupplierId());
        
        // Crear contrato
        Contract contract = contractMapper.toEntity(request, userId);
        
        // Generar número de contrato único
        contract.setContractNumber(generateContractNumber());
        
        // Guardar datos desnormalizados del proveedor
        contract.setSupplierNit(supplier.getNit());
        contract.setSupplierBusinessName(supplier.getBusinessName());
        
        Contract savedContract = contractRepository.save(contract);
        
        // Registrar primer estado en historial
        registerStatusHistory(savedContract, null, ContractStatus.BORRADOR, "Contract created", userId);
        
        log.info("Contract created with ID: {}, Number: {}", savedContract.getId(), savedContract.getContractNumber());
        return contractMapper.toResponse(savedContract);
    }
    
    /**
     * Actualizar contrato (solo si está en estado BORRADOR)
     */
    public ContractResponse updateContract(UUID id, ContractUpdateRequest request, 
                                           UUID userId, String userRole, String userEmail) {
        log.info("Updating contract with ID: {} by user: {}", id, userId);
        
        Contract contract = findContractById(id);
        
        // Validar que esté en BORRADOR para editar
        if (contract.getStatus() != ContractStatus.BORRADOR) {
            throw new ContractNotEditableException(ERROR_NOT_EDITABLE, 
                "Contract can only be edited in BORRADOR state. Current state: " + contract.getStatus());
        }
        
        // Verificar permisos (propietario o ADMIN)
        checkEditPermission(contract, userId, userRole);
        
        contractMapper.updateEntity(contract, request);
        Contract updatedContract = contractRepository.save(contract);
        
        log.info("Contract updated with ID: {}", id);
        return contractMapper.toResponse(updatedContract);
    }
    
    /**
     * Cambiar estado de contrato
     * Reglas: transiciones válidas según máquina de estados
     */
    public ContractResponse changeStatus(UUID id, StatusChangeRequest request, 
                                         UUID userId, String userRole, String userEmail) {
        log.info("Changing contract status. ID: {}, New status: {}, User: {}", id, request.getNewStatus(), userId);
        
        Contract contract = findContractById(id);
        ContractStatus oldStatus = contract.getStatus();
        ContractStatus newStatus = request.getNewStatus();
        
        // Solo ADMIN puede cambiar estados 
        if (!"ADMINISTRADOR".equals(userRole)) {
            throw new AccessDeniedException("Only ADMIN can change contract status");
        }
        
        // Validar transición
        if (!stateMachine.isValidTransition(oldStatus, newStatus)) {
            throw new InvalidStatusTransitionException(ERROR_INVALID_TRANSITION, 
                String.format("Invalid status transition from %s to %s", oldStatus, newStatus));
        }
        
        // Actualizar estado
        contract.setStatus(newStatus);
        Contract updatedContract = contractRepository.save(contract);
        
        // Registrar en historial local
        registerStatusHistory(contract, oldStatus, newStatus, request.getReason(), userId);
        
        // Enviar evento a Auditoría externa
        try {
            EventoAuditoriaDTO evento = EventoAuditoriaDTO.builder()
                .contrato_id(id)
                .tipo_evento("CAMBIAR_ESTADO")
                .estado_anterior(oldStatus.toString())
                .estado_nuevo(newStatus.toString())
                .motivo(request.getReason() != null ? request.getReason() : "")
                .usuario_id(userId.toString())
                .usuario_nombre(userEmail)
                .rol_usuario(userRole)
                .fecha(LocalDateTime.now())
                .build();
            
            auditClient.registrarEvento(evento);
            log.info("Audit event sent for contract status change. Contract ID: {}", id);
        } catch (Exception e) {
            // Error de auditoría no debe bloquear el cambio de estado
            log.warn("Failed to send audit event for contract status change. Contract ID: {}, Error: {}", id, e.getMessage());
        }
        
        log.info("Status changed from {} to {} for contract: {}", oldStatus, newStatus, id);
        
        return contractMapper.toResponse(updatedContract);
    }
    
    /**
     * Eliminar contrato (baja lógica)
     * Solo si está en estado BORRADOR
     */
    public void deleteContract(UUID id, UUID userId, String userRole) {
        log.info("Deleting contract with ID: {} by user: {}", id, userId);
        
        Contract contract = findContractById(id);
        
        if (contract.getStatus() != ContractStatus.BORRADOR) {
            throw new ContractNotEditableException(ERROR_NOT_EDITABLE, 
                "Only contracts in BORRADOR state can be deleted");
        }
        
        checkEditPermission(contract, userId, userRole);
        
        contract.setDeleted(true);
        contractRepository.save(contract);
        
        log.info("Contract deleted (soft delete) with ID: {}", id);
    }
    
    /**
     * Listar contratos con filtros
     * ADMIN: ve todos
     * FUNCIONARIO: solo sus contratos
     * AUDITOR: ve todos (solo lectura)
     */
    @Transactional(readOnly = true)
    public Page<ContractResponse> listContracts(ContractStatus status, String search, 
                                                 Pageable pageable, UUID userId, String userRole) {
        log.debug("Listing contracts. User: {}, Role: {}", userId, userRole);
        
        Page<Contract> contracts;
        
        if ("FUNCIONARIO".equals(userRole)) {
            // FUNCIONARIO solo ve sus propios contratos
            contracts = contractRepository.findByUserIdWithFilters(userId, status, pageable);
        } else {
            // ADMIN y AUDITOR ven todos
            contracts = contractRepository.findAllWithFilters(status, search, pageable);
        }
        
        return contracts.map(contractMapper::toResponse);
    }
    
    /**
     * Obtener contrato por ID
     */
    @Transactional(readOnly = true)
    public ContractResponse getContractById(UUID id, UUID userId, String userRole) {
        Contract contract = findContractById(id);
        
        // Verificar acceso (FUNCIONARIO solo puede ver sus contratos)
        if ("FUNCIONARIO".equals(userRole) && !contract.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to view this contract");
        }
        
        return contractMapper.toResponse(contract);
    }
    
    /**
     * Obtener historial de estados de un contrato
     */
    @Transactional(readOnly = true)
    public List<ContractStatusHistoryResponse> getContractHistory(UUID id, UUID userId, String userRole) {
        Contract contract = findContractById(id);
        
        // Verificar acceso
        if ("FUNCIONARIO".equals(userRole) && !contract.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to view this contract history");
        }
        
        List<ContractStatusHistory> history = historyRepository.findByContractOrderByChangeDateAsc(contract);
        return history.stream()
                .map(contractMapper::toHistoryResponse)
                .toList();
    }
    
    /**
     * Obtener contratos activos de un proveedor (para validación de desactivación)
     */
    @Transactional(readOnly = true)
    public List<ContractSummaryDTO> getActiveContractsBySupplier(UUID supplierId) {
        List<ContractStatus> activeStatuses = List.of(ContractStatus.ACTIVO, ContractStatus.EN_EJECUCION);
        
        List<Contract> contracts = contractRepository.findBySupplierIdAndStatusInAndDeletedFalse(supplierId, activeStatuses);
        
        return contracts.stream()
                .map(c -> ContractSummaryDTO.builder()
                        .id(c.getId())
                        .contractNumber(c.getContractNumber())
                        .status(c.getStatus().toString())
                        .build())
                .toList();
    }
    
    // METODOS PRIVADOS 
    
    private Contract findContractById(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(ERROR_NOT_FOUND, 
                    "Contract not found with ID: " + id));
    }
    
    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
    }
    
    private String generateContractNumber() {
        // Formato: CON-YYYY-XXXXX
        String year = String.valueOf(Year.now().getValue());
        long count = contractRepository.count() + 1;
        String sequence = String.format("%05d", count);
        String contractNumber = "CON-" + year + "-" + sequence;
        
        // Verificar unicidad
        if (contractRepository.existsByContractNumber(contractNumber)) {
            return generateContractNumber(); // Recursivo para evitar duplicados
        }
        
        return contractNumber;
    }
    
    private SupplierResponse validateSupplier(UUID supplierId) {
        try {
            SupplierResponse supplier = supplierClient.getSupplierById(supplierId);
            
            if (!"ACTIVO".equals(supplier.getStatus())) {
                throw new SupplierInactiveException(ERROR_SUPPLIER_INACTIVE, 
                    "Supplier is not active. Cannot create contract.");
            }
            
            return supplier;
            
        } catch (FeignException.NotFound e) {
            throw new SupplierNotFoundException(ERROR_SUPPLIER_NOT_FOUND, 
                "Supplier not found with ID: " + supplierId);
        } catch (FeignException e) {
            log.error("Error calling supplier service: {}", e.getMessage());
            throw new RuntimeException("Supplier service unavailable", e);
        }
    }
    
    private void registerStatusHistory(Contract contract, ContractStatus oldStatus, 
                                       ContractStatus newStatus, String reason, UUID userId) {
        ContractStatusHistory history = new ContractStatusHistory();
        history.setContract(contract);
        history.setPreviousStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);
        history.setUserId(userId);
        
        historyRepository.save(history);
        log.debug("Status history recorded for contract: {}", contract.getId());
    }
    
    private void checkEditPermission(Contract contract, UUID userId, String userRole) {
        boolean isOwner = contract.getCreatedByUserId().equals(userId);
        boolean isAdmin = "ADMINISTRADOR".equals(userRole);
        
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You don't have permission to edit this contract");
        }
    }
}