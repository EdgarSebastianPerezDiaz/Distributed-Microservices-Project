package com.distribuidos.usuario_service.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para almacenar Refresh Tokens de OAuth 2.0 en la base de datos
 * Permite:
 * - Persistencia de tokens para recuperación ante fallos
 * - Revocación de tokens
 * - Auditoría de sesiones
 * - Gestión de renovación de tokens
 */
@Entity
@Table(name = "oauth_refresh_tokens")
public class OAuthRefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "token_value", nullable = false, unique = true, columnDefinition = "TEXT")
    private String tokenValue;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "revoked", nullable = false)
    private Boolean revoked = false;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public OAuthRefreshToken() {}
    
    public OAuthRefreshToken(String tokenValue, User user, String clientId, LocalDateTime expiresAt) {
        this.tokenValue = tokenValue;
        this.user = user;
        this.clientId = clientId;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }
    
    // Getters y Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getTokenValue() {
        return tokenValue;
    }
    
    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Boolean getRevoked() {
        return revoked;
    }
    
    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Verifica si el token está expirado
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Verifica si el token es válido (no revocado y no expirado)
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
