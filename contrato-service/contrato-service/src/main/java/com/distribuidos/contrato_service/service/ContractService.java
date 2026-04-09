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
import java.util.concurrent.atomic.AtomicLong;

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

    // Códigos de error
    private static final String ERROR_NOT_FOUND = "CTR_001";
    private static final String ERROR_NOT_EDITABLE = "CTR_004";
    private static final String ERROR_INVALID_TRANSITION = "CTR_005";
    private static final String ERROR_SUPPLIER_NOT_FOUND = "CTR_006";
    private static final String ERROR_SUPPLIER_INACTIVE = "CTR_007";

    private static final AtomicLong sequenceCounter = new AtomicLong(1);

    /**
     * Crear un nuevo contrato
     * Estado inicial: EN_PREPARACION
     * Objeto debe tener al menos 200 caracteres
     * Proveedor debe existir y estar HABILITADO (ACTIVO)
     */
    public ContractResponse createContract(ContractRequest request, UUID userId, String userRole, String userEmail) {
        log.info("Creating new contract for supplier: {} by user: {}", request.getSupplierId(), userId);

        // Validar fechas
        validateDates(request.getStartDate(), request.getEndDate());

        // Validar objeto tiene mínimo 200 caracteres (ya lo valida @Size, pero validación adicional)
        if (request.getObject().length() < 200) {
            throw new IllegalArgumentException("Contract object must be at least 200 characters");
        }

        // Validar proveedor existe y está ACTIVO
        SupplierResponse supplier = validateSupplier(request.getSupplierId());

        // Crear contrato
        Contract contract = contractMapper.toEntity(request, userId);

        // Estado inicial EN_PREPARACION
        contract.setStatus(ContractStatus.EN_PREPARACION);

        // Generar número de contrato secuencial
        contract.setContractNumber(generateContractNumber());

        // Guardar datos desnormalizados del proveedor
        contract.setSupplierNit(supplier.getNit());
        contract.setSupplierBusinessName(supplier.getBusinessName());

        Contract savedContract = contractRepository.save(contract);

        // Registrar primer estado en historial
        registerStatusHistory(savedContract, null, ContractStatus.EN_PREPARACION, "Contract created", userId);

        // Enviar evento a Auditoría (versión 1)
        sendAuditEvent(savedContract.getId(), "CREAR_CONTRATO", null,
                ContractStatus.EN_PREPARACION.toString(), "Contract created",
                userId.toString(), userEmail, userRole, 1);

        log.info("Contract created with ID: {}, Number: {}", savedContract.getId(), savedContract.getContractNumber());
        return contractMapper.toResponse(savedContract);
    }

    /**
     * Actualizar contrato (solo si está en EN_PREPARACION)
     * Solo se puede modificar: objeto y presupuesto
     * ID, número, fechas y proveedor son inmutables
     */
    public ContractResponse updateContract(UUID id, ContractUpdateRequest request,
                                           UUID userId, String userRole, String userEmail) {
        log.info("Updating contract with ID: {} by user: {}", id, userId);

        // Solo FUNCIONARIO puede modificar contratos
        if (!"FUNCIONARIO".equals(userRole)) {
            throw new AccessDeniedException("Only FUNCIONARIO can modify contracts");
        }

        Contract contract = findContractById(id);

        // Validar que esté en EN_PREPARACION para editar
        if (contract.getStatus() != ContractStatus.EN_PREPARACION) {
            throw new ContractNotEditableException(ERROR_NOT_EDITABLE,
                    "Contract can only be edited in EN_PREPARACION state. Current state: " + contract.getStatus());
        }

        // Verificar permisos (propietario o ADMIN? Según RN-19 solo FUNCIONARIO, no ADMIN)
        if (!contract.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to edit this contract");
        }


        if (request.getBudget() != null) {
            if (request.getBudget().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Budget must be greater than or equal to 0");
            }
            contract.setBudget(request.getBudget());
        }

        Contract updatedContract = contractRepository.save(contract);

        // Enviar evento a Auditoría (versión incrementada)
        sendAuditEvent(contract.getId(), "MODIFICAR_CONTRATO", null, null,
                "Contract updated", userId.toString(), userEmail, userRole,
                getNextVersion(contract.getId()));

        log.info("Contract updated with ID: {}", id);
        return contractMapper.toResponse(updatedContract);
    }

    /**
     * Cambiar estado de contrato
     * Solo FUNCIONARIO puede cambiar estado (según RN-21)
     * Transiciones: EN_PREPARACION → PUBLICADO → ADJUDICADO → EN_EJECUCION → FINALIZADO
     * CANCELADO desde cualquier estado
     */
    public ContractResponse changeStatus(UUID id, StatusChangeRequest request,
                                         UUID userId, String userRole, String userEmail) {
        log.info("Changing contract status. ID: {}, New status: {}, User: {}", id, request.getNewStatus(), userId);

        // Solo FUNCIONARIO puede cambiar estados (según RN-21)
        if (!"FUNCIONARIO".equals(userRole)) {
            throw new AccessDeniedException("Only FUNCIONARIO can change contract status");
        }

        Contract contract = findContractById(id);
        ContractStatus oldStatus = contract.getStatus();
        ContractStatus newStatus = request.getNewStatus();

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

        // Obtener siguiente versión para auditoría
        int nextVersion = getNextVersion(contract.getId());

        // Enviar evento a Auditoría
        sendAuditEvent(contract.getId(), "CAMBIAR_ESTADO", oldStatus.toString(),
                newStatus.toString(), request.getReason(),
                userId.toString(), userEmail, userRole, nextVersion);

        log.info("Status changed from {} to {} for contract: {}", oldStatus, newStatus, id);

        return contractMapper.toResponse(updatedContract);
    }

    /**
     * Eliminar contrato (baja lógica)
     * Solo si está en estado EN_PREPARACION
     */
    public void deleteContract(UUID id, UUID userId, String userRole) {
        log.info("Deleting contract with ID: {} by user: {}", id, userId);

        // Solo FUNCIONARIO puede eliminar
        if (!"FUNCIONARIO".equals(userRole)) {
            throw new AccessDeniedException("Only FUNCIONARIO can delete contracts");
        }

        Contract contract = findContractById(id);

        if (contract.getStatus() != ContractStatus.EN_PREPARACION) {
            throw new ContractNotEditableException(ERROR_NOT_EDITABLE,
                    "Only contracts in EN_PREPARACION state can be deleted");
        }

        if (!contract.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to delete this contract");
        }

        contract.setDeleted(true);
        contractRepository.save(contract);

        log.info("Contract deleted (soft delete) with ID: {}", id);
    }

    /**
     * Listar contratos con filtros
     * FUNCIONARIO: solo sus contratos
     * ADMIN y AUDITOR: ven todos
     */
    @Transactional(readOnly = true)
    public Page<ContractResponse> listContracts(ContractStatus status, String search,
                                                Pageable pageable, UUID userId, String userRole) {
        log.debug("Listing contracts. User: {}, Role: {}", userId, userRole);

        Page<Contract> contracts;

        if ("FUNCIONARIO".equals(userRole)) {
            contracts = contractRepository.findByUserIdWithFilters(userId, status, pageable);
        } else {
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

        if ("FUNCIONARIO".equals(userRole) && !contract.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to view this contract history");
        }

        List<ContractStatusHistory> history = historyRepository.findByContractOrderByChangeDateAsc(contract);
        return history.stream()
                .map(contractMapper::toHistoryResponse)
                .toList();
    }

    /**
     * Obtener contratos activos de un proveedor (EN_EJECUCION o PUBLICADO o ADJUDICADO)
     */
    @Transactional(readOnly = true)
    public List<ContractSummaryDTO> getActiveContractsBySupplier(UUID supplierId) {
        List<ContractStatus> activeStatuses = List.of(
                ContractStatus.PUBLICADO,
                ContractStatus.ADJUDICADO,
                ContractStatus.EN_EJECUCION
        );

        List<Contract> contracts = contractRepository.findBySupplierIdAndStatusInAndDeletedFalse(supplierId, activeStatuses);

        return contracts.stream()
                .map(c -> ContractSummaryDTO.builder()
                        .id(c.getId())
                        .contractNumber(c.getContractNumber())
                        .status(c.getStatus().toString())
                        .build())
                .toList();
    }

    // ==================== MÉTODOS PRIVADOS ====================

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
        // Número secuencial: 001, 002, 003, ...
        // Obtener el último número de contrato
        String lastNumber = contractRepository.findTopByOrderByContractNumberDesc()
                .map(Contract::getContractNumber)
                .orElse("000");

        try {
            long lastSeq = Long.parseLong(lastNumber);
            long newSeq = lastSeq + 1;
            return String.format("%03d", newSeq);
        } catch (NumberFormatException e) {
            return String.format("%03d", sequenceCounter.getAndIncrement());
        }
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

    private void sendAuditEvent(UUID contractId, String eventType, String oldStatus,
                                String newStatus, String reason, String userId,
                                String userEmail, String userRole, int version) {
        try {
            EventoAuditoriaDTO evento = EventoAuditoriaDTO.builder()
                    .contrato_id(contractId)
                    .tipo_evento(eventType)
                    .estado_anterior(oldStatus)
                    .estado_nuevo(newStatus)
                    .motivo(reason != null ? reason : "")
                    .usuario_id(userId)
                    .usuario_nombre(userEmail)
                    .rol_usuario(userRole)
                    .version(version)
                    .fecha(LocalDateTime.now())
                    .build();

            auditClient.registrarEvento(evento);
            log.info("Audit event sent: {} for contract: {}", eventType, contractId);
        } catch (Exception e) {
            log.warn("Failed to send audit event for contract: {}, Error: {}", contractId, e.getMessage());
        }
    }

    private int getNextVersion(UUID contractId) {
        // Implementar lógica para obtener la siguiente versión desde audit-service
        // Por ahora retorna un número incremental simple
        return (int) historyRepository.countByContractId(contractId) + 1;
    }
}