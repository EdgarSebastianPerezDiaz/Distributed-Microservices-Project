package com.distribuidos.contrato_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contratos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "proveedor_id", nullable = false)
    private UUID supplierId;

    @Column(name = "numero_contrato", unique = true, nullable = false, length = 50)
    private String contractNumber;

    @NotBlank(message = "Object is required")
    @Column(nullable = false, length = 2000)
    private String object;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal budget;

    @NotNull(message = "Start date is required")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status = ContractStatus.EN_PREPARACION;  // ← Estado inicial por defecto

    @Column(name = "usuario_creador_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "eliminado", nullable = false)
    private boolean deleted = false;

    // Campos desnormalizados de proveedor
    @Column(name = "proveedor_nit", length = 20)
    private String supplierNit;

    @Column(name = "proveedor_razon_social", length = 200)
    private String supplierBusinessName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}