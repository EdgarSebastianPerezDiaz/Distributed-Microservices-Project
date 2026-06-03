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
import com.distribuidos.contrato_service.security.JwtPrincipal;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.time.OffsetDateTime;
import java.time.Year;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        String userId1 = principal.getUserId();
        String userName = principal.getEmail() != null && !principal.getEmail().isBlank()
        ? principal.getEmail()
        : principal.getUsername();
        String userRole1 = principal.getRole();
        // Enviar evento a Auditoría (versión incrementada)
        sendAuditEvent(contract.getId(), "MODIFICAR_CONTRATO", null, null,
                "Contract updated", userId1, userName, userRole1,
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
      //  if (!"FUNCIONARIO".equals(userRole)) {
        //    throw new AccessDeniedException("Only FUNCIONARIO can change contract status");
       // }

        boolean isCancelByAdmin="ADMINISTRADOR".equals(userRole) && request.getNewStatus() == ContractStatus.CANCELADO;
        boolean isFuncionario="FUNCIONARIO".equals(userRole);

        if (!isFuncionario && !isCancelByAdmin){
            throw new AccessDeniedException("Solo Funcionario o Administrador ");
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
        Authentication authentication =  SecurityContextHolder.getContext().getAuthentication();
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        String userName = principal.getEmail() != null && !principal.getEmail().isBlank()
            ? principal.getEmail()
            : principal.getUsername();
         userRole = principal.getRole();
        int version = getNextVersion(contract.getId());

        contract.setDeleted(true);
        contractRepository.save(contract);

        log.info("Contract deleted (soft delete) with ID: {}", id);

        //registro en auditoria
          sendAuditEvent(
            contract.getId(),
            "ELIMINAR_CONTRATO",
            contract.getStatus().toString(),
            "ELIMINADO",
            "Contrato eliminado lógicamente por el funcionario",
            userId.toString(),
            userName,
            userRole,
            version
    );
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
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        if ("FUNCIONARIO".equals(userRole)) {
            contracts = contractRepository.findByUserIdWithFilters(userId, status, pageable);
        } else {
            // For admin/auditor, when no filters are provided use a direct query.
            // This avoids edge cases where optional filter params may return empty data.
            if (status == null && normalizedSearch == null) {
                contracts = contractRepository.findByDeletedFalse(pageable);
            } else {
                contracts = contractRepository.findAllWithFilters(status, normalizedSearch, pageable);
            }
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
     * Generar PDF profesional de contrato por ID
     */
    @Transactional(readOnly = true)
    public byte[] generateContractPdf(UUID id, UUID userId, String userRole) {
        Contract contract = findContractById(id);

        if ("FUNCIONARIO".equals(userRole) && !contract.getCreatedByUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to generate this contract PDF");
        }

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float marginLeft = 40;
            float marginRight = 40;
            float marginTop = 50;
            float marginBottom = 40;
            float contentWidth = pageWidth - marginLeft - marginRight;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            try {
                float yPosition = pageHeight - marginTop;

                // ========== ENCABEZADO ==========
                yPosition = drawHeader(contentStream, "DOCUMENTO DE CONTRATO", marginLeft, yPosition);
                yPosition -= 10;

                // ========== NÚMERO DE CONTRATO (DESTAQUE) ==========
                yPosition = drawContractNumberBox(contentStream, contract.getContractNumber(), marginLeft, contentWidth, yPosition);
                yPosition -= 15;

                // ========== DATOS PRINCIPALES (DOS COLUMNAS) ==========
                yPosition = drawSection(contentStream, "INFORMACIÓN GENERAL", marginLeft, contentWidth, yPosition);
                yPosition -= 12;

                float column1X = marginLeft;
                float column2X = marginLeft + (contentWidth / 2) + 10;
                float columnWidth = (contentWidth / 2) - 5;

                float yCol1 = yPosition;
                float yCol2 = yPosition;

                // Columna 1
                yCol1 -= 16;
                drawField(contentStream, "Nº de Contrato:", safeText(contract.getContractNumber()), column1X, columnWidth, yCol1);
                yCol1 -= 16;
                drawField(contentStream, "ID del Contrato:", truncateUuid(contract.getId().toString()), column1X, columnWidth, yCol1);
                yCol1 -= 16;
                drawField(contentStream, "Estado:", contract.getStatus().toString(), column1X, columnWidth, yCol1);
                yCol1 -= 16;
                drawField(contentStream, "Presupuesto:", "$" + contract.getBudget().setScale(2, RoundingMode.HALF_UP), column1X, columnWidth, yCol1);
                yCol1 -= 16;
                drawField(contentStream, "Fecha Inicio:", contract.getStartDate().toString(), column1X, columnWidth, yCol1);

                // Columna 2
                yCol2 -= 16;
                drawField(contentStream, "Proveedor:", safeText(contract.getSupplierBusinessName()), column2X, columnWidth, yCol2);
                yCol2 -= 16;
                drawField(contentStream, "NIT Proveedor:", safeText(contract.getSupplierNit()), column2X, columnWidth, yCol2);
                yCol2 -= 16;
                drawField(contentStream, "ID Proveedor:", truncateUuid(contract.getSupplierId().toString()), column2X, columnWidth, yCol2);
                yCol2 -= 16;
                drawField(contentStream, "Fecha Fin:", contract.getEndDate().toString(), column2X, columnWidth, yCol2);
                yCol2 -= 16;
                drawField(contentStream, "Creado:", contract.getCreatedAt().toString().substring(0, 10), column2X, columnWidth, yCol2);

                yPosition = Math.min(yCol1, yCol2) - 20;

                // ========== OBJETO DEL CONTRATO ==========
                yPosition = drawSection(contentStream, "OBJETO DEL CONTRATO", marginLeft, contentWidth, yPosition);
                yPosition -= 12;

                String objectText = safeText(contract.getObject());
                for (String line : wrapTextToWidth(objectText, contentWidth, 10)) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    contentStream.newLineAtOffset(marginLeft + 10, yPosition);
                    contentStream.showText(line);
                    contentStream.endText();
                    yPosition -= 14;

                    // Si se acerca al pie de página, crear nueva página
                    if (yPosition < marginBottom + 100) {
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = pageHeight - marginTop;
                    }
                }

                // ========== LÍNEA FINAL ==========
                yPosition -= 10;
                contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
                contentStream.setLineWidth(1);
                contentStream.moveTo(marginLeft, yPosition);
                contentStream.lineTo(pageWidth - marginRight, yPosition);
                contentStream.stroke();

                // ========== PIE DE PÁGINA ==========
                yPosition -= 15;
                drawFooter(contentStream, "Documento generado automáticamente - " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), marginLeft, contentWidth, yPosition);

                contentStream.close();
            } catch (IOException e) {
                try {
                    contentStream.close();
                } catch (IOException ex) {
                    log.warn("Error closing content stream", ex);
                }
                throw e;
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            log.error("Error generating PDF for contract {}", id, e);
            throw new RuntimeException("Could not generate contract PDF", e);
        }
    }

    private float drawHeader(PDPageContentStream contentStream, String title, float x, float y) throws IOException {
        contentStream.setStrokingColor(0.2f, 0.4f, 0.8f);
        contentStream.setLineWidth(2);
        contentStream.moveTo(x, y - 5);
        contentStream.lineTo(x + 200, y - 5);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
        contentStream.setNonStrokingColor(0.2f, 0.4f, 0.8f);
        contentStream.newLineAtOffset(x, y - 20);
        contentStream.showText(title);
        contentStream.endText();

        return y - 25;
    }

    private float drawContractNumberBox(PDPageContentStream contentStream, String contractNumber, float x, float width, float y) throws IOException {
        float boxHeight = 25;
        contentStream.setNonStrokingColor(0.9f, 0.95f, 1.0f);
        contentStream.fillRect(x, y - boxHeight, width, boxHeight);

        contentStream.setStrokingColor(0.2f, 0.4f, 0.8f);
        contentStream.setLineWidth(1);
        contentStream.addRect(x, y - boxHeight, width, boxHeight);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        contentStream.setNonStrokingColor(0.2f, 0.4f, 0.8f);
        contentStream.newLineAtOffset(x + 10, y - 18);
        contentStream.showText("Contrato Nº " + contractNumber);
        contentStream.endText();

        return y - boxHeight;
    }

    private float drawSection(PDPageContentStream contentStream, String title, float x, float width, float y) throws IOException {
        contentStream.setNonStrokingColor(0.95f, 0.95f, 0.95f);
        contentStream.fillRect(x, y - 18, width, 18);

        contentStream.setStrokingColor(0.5f, 0.5f, 0.5f);
        contentStream.setLineWidth(0.5f);
        contentStream.addRect(x, y - 18, width, 18);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
        contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f);
        contentStream.newLineAtOffset(x + 8, y - 13);
        contentStream.showText(title);
        contentStream.endText();

        return y - 18;
    }

    private void drawField(PDPageContentStream contentStream, String label, String value, float x, float width, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 9);
        contentStream.setNonStrokingColor(0.2f, 0.4f, 0.8f);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label);
        contentStream.endText();

        String truncatedValue = value;
        if (value.length() > 40) {
            truncatedValue = value.substring(0, 37) + "...";
        }

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.setNonStrokingColor(0.3f, 0.3f, 0.3f);
        contentStream.newLineAtOffset(x + 120, y);
        contentStream.showText(truncatedValue);
        contentStream.endText();
    }

    private void drawFooter(PDPageContentStream contentStream, String text, float x, float width, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
        contentStream.setNonStrokingColor(0.6f, 0.6f, 0.6f);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private List<String> wrapTextToWidth(String text, float width, float fontSize) {
        if (text == null || text.isBlank()) {
            return List.of("-");
        }

        List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        int maxCharsPerLine = (int) (width / (fontSize * 0.6f));

        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxCharsPerLine) {
                current.append(' ').append(word);
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    private String truncateUuid(String uuid) {
        if (uuid == null || uuid.length() < 8) {
            return uuid;
        }
        return uuid.substring(0, 8) + "...";
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

            if (!("HABILITADO".equals(supplier.getStatus())
      || "ACTIVO".equals(supplier.getStatus()))) {
    throw new SupplierInactiveException(
        ERROR_SUPPLIER_INACTIVE,
        "Supplier is not active. Cannot create contract."
    );
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
                    .entidad_tipo("CONTRATO")
                    .entidad_id(contractId.toString())
                    .tipo_evento(eventType)
                    .descripcion(reason !=null && !reason.isBlank() ? reason : eventType)
                    .estado_anterior(oldStatus)
                    .estado_nuevo(newStatus)
                    .motivo(reason != null ? reason : "")
                    .usuario_id(userId)
                    .usuario_nombre( userEmail != null && !userEmail.isBlank()
                        ? userEmail
                        : "usuario_desconocido")
                    .rol_usuario(userRole)
                    .version(version)
                .fecha(OffsetDateTime.now(java.time.ZoneOffset.UTC))
                    .build();

         boolean sent= auditClient.registrarEvento(evento);

if (sent) {
    log.info("Audit event sent: {} for contract: {}", eventType, contractId);
} else {
    log.warn("Audit event could not be recorded for contract: {}", contractId);
}


            log.info("Audit event sent: {} for contract: {}", eventType, contractId);
        } catch (Exception e) {
            log.warn("Failed to send audit event for contract: {}, Error: {}", contractId, e.getMessage());
        }
    }

    private int getNextVersion(UUID contractId) {
        // Implementar lógica para obtener la siguiente versión desde audit-service
        // Por ahora retorna un número incremental simple
         long historialCount = historyRepository.countByContractId(contractId);
    return (int) historialCount + 2;
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim();
    }
}