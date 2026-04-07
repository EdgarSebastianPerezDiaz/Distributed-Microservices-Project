package com.distribuidos.usuario_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad Role - Tabla de Roles
 * 
 * Almacena los 3 roles del sistema:
 * - ADMINISTRADOR: Puede crear usuarios y ver todo
 * - FUNCIONARIO: Opera con contratos
 * - AUDITOR: Solo lectura de auditoría
 * 
 * Relación: Un usuario tiene exactamente UN rol (no multirol)
 * Esto es un requisito crítico del sistema.
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-incremental
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;  // ADMINISTRADOR, FUNCIONARIO, AUDITOR
    
    @Column(length = 255)
    private String description;
}