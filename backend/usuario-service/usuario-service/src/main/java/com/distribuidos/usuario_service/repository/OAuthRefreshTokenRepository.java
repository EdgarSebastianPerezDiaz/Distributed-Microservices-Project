package com.distribuidos.usuario_service.repository;

import com.distribuidos.usuario_service.model.OAuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthRefreshTokenRepository extends JpaRepository<OAuthRefreshToken, UUID> {
    
    /**
     * Buscar token por su valor
     */
    Optional<OAuthRefreshToken> findByTokenValue(String tokenValue);
    
    /**
     * Buscar tokens de un usuario
     */
    List<OAuthRefreshToken> findByUser_Id(UUID userId);
    
    /**
     * Buscar tokens de un cliente específico para un usuario
     */
    List<OAuthRefreshToken> findByUser_IdAndClientId(UUID userId, String clientId);
    
    /**
     * Revocar todos los tokens de un usuario
     */
    @Query("UPDATE OAuthRefreshToken t SET t.revoked = true WHERE t.user.id = ?1")
    int revokeAllByUserId(UUID userId);
    
    /**
     * Revocar todos los tokens expirados (limpieza)
     */
    @Query("DELETE FROM OAuthRefreshToken t WHERE t.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpiredTokens();
    
    /**
     * Contar tokens válidos de un usuario
     */
    @Query("SELECT COUNT(t) FROM OAuthRefreshToken t WHERE t.user.id = ?1 AND t.revoked = false AND t.expiresAt > CURRENT_TIMESTAMP")
    long countValidTokensByUserId(UUID userId);
}
