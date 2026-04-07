package com.distribuidos.usuario_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad User - Tabla de Usuarios
 * 
 * Campos importantes:
 * - id: UUID generado automáticamente (no es email ni username)
 * - username: Para login (único)
 * - passwordHash: SHA-512 en hexadecimal (128 caracteres exactos)
 * - email: Para contacto (único)
 * - role: Relación ManyToOne (muchos usuarios pueden tener mismo rol)
 * - active: Soft delete (no borramos, desactivamos)
 * 
 * Restricción CRÍTICA: No multirol. Un usuario tiene exactamente un rol_id.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // UUID automático
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;  // Ej: jperez, admin, maria.garcia
    
    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;  // SHA-512 siempre son 128 caracteres hex
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;  // Nombre completo
    
    // RELACIÓN CRÍTICA: Un usuario tiene UN solo rol
    @ManyToOne(fetch = FetchType.EAGER)  // EAGER para cargar el rol siempre
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @Column(nullable = false)
    private Boolean active = true;  // true = activo, false = desactivado
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;  // Último acceso exitoso
    
    /**
     * Método helper para obtener el nombre del rol como String
     */
    public String getRoleName() {
        return role != null ? role.getName() : null;
    }
}