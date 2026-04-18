package com.distribuidos.proveedor_service.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.distribuidos.proveedor_service.client.AuditClient;
import com.distribuidos.proveedor_service.client.ContractClient;
import com.distribuidos.proveedor_service.dto.AuditEventDTO;
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
import com.distribuidos.proveedor_service.security.JwtPrincipal;

import feign.FeignException;
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
    private final AuditClient auditClient;

    // Códigos de error
    private static final String ERROR_NOT_FOUND = "PRV_001";
    private static final String ERROR_DUPLICATE_NIT = "PRV_002";
    private static final String ERROR_DUPLICATE_EMAIL = "PRV_002";
    private static final String ERROR_HAS_ACTIVE_CONTRACTS = "PRV_003";

    /**
     * Crear nuevo proveedor
     * Validar unicidad de NIT y email
     * Estado inicial: ACTIVO (HABILITADO)
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
        supplier.setStatus(SupplierStatus.ACTIVO); // Estado inicial HABILITADO
        Supplier savedSupplier = supplierRepository.save(supplier);

         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
    String userId   = principal.getUserId();
    String userName = (principal.getEmail() != null && !principal.getEmail().isBlank())
                      ? principal.getEmail() : principal.getUsername();
    String userRole = principal.getRole();

        // Enviar evento de auditoría para creación
       sendAuditEvent(
            savedSupplier.getId(),
            "CREAR_PROVEEDOR",
            null,
            "ACTIVO",
            "Nuevo proveedor registrado: " + savedSupplier.getBusinessName(),
            userId,     
            userName,  
            userRole,   
            1
    );

       
        return supplierMapper.toResponse(savedSupplier);
    }

    /**
     * Actualizar proveedor existente
     * Solo se pueden modificar: razón social, teléfono, estado
     * ID y NIT son inmutables
     * Cada modificación genera evento de auditoría
     */
    public SupplierResponse updateSupplier(UUID id, SupplierUpdateRequest request, String userId, String userEmail, String userRole) {
        log.info("Updating supplier with ID: {}", id);
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
     userId   = principal.getUserId();
    String userName = (principal.getEmail() != null && !principal.getEmail().isBlank())
                      ? principal.getEmail() : principal.getUsername();
     userRole = principal.getRole();


        // Solo ADMIN puede modificar proveedores (según RF-PROV-03, FUNCIONARIO solo lectura)
        if (!"ADMINISTRADOR".equals(userRole)) {
            throw new AccessDeniedException("Only ADMIN can modify suppliers");
        }

        Supplier existingSupplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(ERROR_NOT_FOUND,
                        "Supplier not found with ID: " + id));

        String oldBusinessName = existingSupplier.getBusinessName();
        String oldPhone = existingSupplier.getPhone();
        SupplierStatus oldStatus = existingSupplier.getStatus();
        String oldEmail = existingSupplier.getEmail();

        // Validar email único (excluyendo el propio)
        if (request.getEmail() != null && !request.getEmail().equals(existingSupplier.getEmail())) {
            if (supplierRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new DuplicateSupplierException(ERROR_DUPLICATE_EMAIL, "email",
                        "Email already exists: " + request.getEmail());
            }
        }

        // Actualizar solo campos permitidos (businessName, email, phone, status)
        supplierMapper.updateEntity(existingSupplier, request);
        Supplier updatedSupplier = supplierRepository.save(existingSupplier);

        // Enviar evento de auditoría
        String changes = buildChangeDescription(oldBusinessName, updatedSupplier.getBusinessName(),
                oldPhone, updatedSupplier.getPhone(),
                oldEmail, updatedSupplier.getEmail(),
                oldStatus, updatedSupplier.getStatus());

        sendAuditEvent(updatedSupplier.getId(), "MODIFICAR_PROVEEDOR", null, null,
                changes, userId, userEmail, userRole, getNextVersion(updatedSupplier.getId()));

                 sendAuditEvent(
            updatedSupplier.getId(),
            "MODIFICAR_PROVEEDOR",
            oldStatus.toString(),
            updatedSupplier.getStatus().toString(),
            changes.isBlank() ? "Proveedor modificado sin cambios detectados" : changes,
            userId,
            userName,
            userRole,
            getNextVersion(updatedSupplier.getId())
    );
        log.info("Supplier updated with ID: {}", id);
        return supplierMapper.toResponse(updatedSupplier);
    }

    /**
     * Cambiar estado de proveedor (ACTIVO/INACTIVO)
     * Si se inactiva, validar que no tenga contratos activos
     * Cada cambio genera evento de auditoría
     */
    public SupplierResponse changeSupplierStatus(UUID id, SupplierStatus newStatus,
                                                 String userId, String userEmail, String userRole) {
        log.info("Changing supplier status. ID: {}, New status: {}, User: {}", id, newStatus, userId);

        // Solo ADMIN puede cambiar estado
        if (!"ADMINISTRADOR".equals(userRole)) {
            throw new AccessDeniedException("Only ADMIN can change supplier status");
        }

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
            try {
                List<ContractSummaryDTO> activeContracts = contractClient.getActiveContractsBySupplier(id.toString());
                if (activeContracts != null && !activeContracts.isEmpty()) {
                    throw new SupplierHasActiveContractsException(ERROR_HAS_ACTIVE_CONTRACTS,
                            "Cannot deactivate supplier with active contracts. Active contracts count: " + activeContracts.size());
                }
            } catch (FeignException e) {
                log.error("Failed to validate contracts for supplier {}: {}", id, e.getMessage());
                throw new RuntimeException("Contract service unavailable. Cannot validate active contracts.");
            }
        }

        supplier.setStatus(newStatus);
        Supplier updatedSupplier = supplierRepository.save(supplier);

        // Enviar evento de auditoría
        sendAuditEvent(updatedSupplier.getId(), "CAMBIAR_ESTADO_PROVEEDOR",
                oldStatus.toString(), newStatus.toString(),
                "Supplier status changed", userId, userEmail, userRole,
                getNextVersion(updatedSupplier.getId()));

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

        // Convertir enums a String para el query nativo
        String statusStr = status != null ? status.toString() : null;
        String personTypeStr = personType != null ? personType.toString() : null;

        // Obtener total de registros
        long total = supplierRepository.countAllWithFilters(statusStr, personTypeStr, search);

        // Obtener la lista paginada manualmente
        List<Supplier> suppliers = supplierRepository.findAllWithFiltersList(statusStr, personTypeStr, search);

        // Aplicar paginación manual
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), suppliers.size());

        List<Supplier> paginatedList = suppliers.subList(start, end);

        // Crear Page object
        Page<Supplier> page = new PageImpl<>(paginatedList, pageable, total);

        return page.map(supplierMapper::toResponse);
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

    // ==================== MÉTODOS PRIVADOS ====================

    private void sendAuditEvent(UUID entityId, String eventType, String oldStatus,
                                String newStatus, String reason, String userId,
                                String userEmail, String userRole, int version) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

 userId   = principal.getUserId();
        String userName = principal.getEmail() != null && !principal.getEmail().isBlank()
                          ? principal.getEmail()
                          : principal.getUsername();
         userRole = principal.getRole();


            AuditEventDTO evento = AuditEventDTO.builder()
                    .proveedor_id(entityId)
                    .entidad_tipo("PROVEEDOR")
                    .entidad_id(entityId.toString())
                    .tipo_evento(eventType)
                    .descripcion(reason != null && !reason.isBlank() ? reason : eventType)
                    .estado_anterior(oldStatus)
                    .estado_nuevo(newStatus)
                    .motivo(reason != null ? reason : "")
                    .usuario_id(userId)
                    .usuario_nombre(userEmail)
                    .rol_usuario(userRole)
                    .version(version)
                   .fecha(OffsetDateTime.now(java.time.ZoneOffset.UTC))
                    .build();

            auditClient.registrarEvento(evento);
            log.info("Audit event sent: {} for supplier: {}", eventType, entityId);
        } catch (Exception e) {
            log.warn("Failed to send audit event for supplier: {}, Error: {}", entityId, e.getMessage());
        }
    }

    private int getNextVersion(UUID supplierId) {
        // Implementar lógica para obtener la siguiente versión
        return 1; // Simplificado, en producción debería venir del audit-service
    }

    private String buildChangeDescription(String oldBusinessName, String newBusinessName,
                                          String oldPhone, String newPhone,
                                          String oldEmail, String newEmail,
                                          SupplierStatus oldStatus, SupplierStatus newStatus) {
        StringBuilder changes = new StringBuilder();
        if (oldBusinessName != null && !oldBusinessName.equals(newBusinessName)) {
            changes.append("businessName: '").append(oldBusinessName).append("' -> '").append(newBusinessName).append("'; ");
        }
        if (oldPhone != null && !oldPhone.equals(newPhone)) {
            changes.append("phone: '").append(oldPhone).append("' -> '").append(newPhone).append("'; ");
        }
        if (oldEmail != null && !oldEmail.equals(newEmail)) {
            changes.append("email: '").append(oldEmail).append("' -> '").append(newEmail).append("'; ");
        }
        if (oldStatus != null && oldStatus != newStatus) {
            changes.append("status: '").append(oldStatus).append("' -> '").append(newStatus).append("'; ");
        }
        return changes.toString();
    }
}