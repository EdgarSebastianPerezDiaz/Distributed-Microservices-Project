package com.distribuidos.contrato_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contratos_historial_estados")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private ContractStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false)
    private ContractStatus newStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "usuario_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime changeDate;
}