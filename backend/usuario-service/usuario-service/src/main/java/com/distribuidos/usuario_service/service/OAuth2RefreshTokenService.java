package com.distribuidos.usuario_service.service;

import com.distribuidos.usuario_service.model.OAuthRefreshToken;
import com.distribuidos.usuario_service.model.User;
import com.distribuidos.usuario_service.repository.OAuthRefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para gestionar Refresh Tokens de OAuth 2.0
 * 
 * Responsabilidades:
 * - Almacenar refresh tokens en BD PostgreSQL
 * - Validar refresh tokens
 * - Revocar tokens
 * - Limpiar tokens expirados (scheduled job)
 */
@Service
public class OAuth2RefreshTokenService {
    
    private final OAuthRefreshTokenRepository refreshTokenRepository;
    
    public OAuth2RefreshTokenService(OAuthRefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }
    
    /**
     * Guardar un refresh token en la base de datos
     */
    @Transactional
    public OAuthRefreshToken saveRefreshToken(String tokenValue, User user, String clientId, LocalDateTime expiresAt) {
        OAuthRefreshToken refreshToken = new OAuthRefreshToken(tokenValue, user, clientId, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }
    
    /**
     * Buscar y validar un refresh token
     * 
     * @param tokenValue Valor del refresh token
     * @return OAuthRefreshToken si es válido, o null si no existe o está revocado/expirado
     */
    public OAuthRefreshToken findValidRefreshToken(String tokenValue) {
        var token = refreshTokenRepository.findByTokenValue(tokenValue);
        
        if (token.isEmpty()) {
            return null;
        }
        
        OAuthRefreshToken refreshToken = token.get();
        
        // Validar que no esté revocado y no esté expirado
        if (refreshToken.isValid()) {
            return refreshToken;
        }
        
        return null;
    }
    
    /**
     * Revocar un refresh token específico
     */
    @Transactional
    public void revokeRefreshToken(String tokenValue) {
        var token = refreshTokenRepository.findByTokenValue(tokenValue);
        if (token.isPresent()) {
            OAuthRefreshToken refreshToken = token.get();
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        }
    }
    
    /**
     * Revocar todos los refresh tokens de un usuario
     * (útil cuando el usuario cambia contraseña o cierra sesión en todos lados)
     */
    @Transactional
    public int revokeAllRefreshTokensForUser(UUID userId) {
        return refreshTokenRepository.revokeAllByUserId(userId);
    }
    
    /**
     * Obtener todos los refresh tokens válidos de un usuario
     */
    public List<OAuthRefreshToken> getValidRefreshTokensForUser(UUID userId) {
        return refreshTokenRepository.findByUser_Id(userId)
            .stream()
            .filter(OAuthRefreshToken::isValid)
            .toList();
    }
    
    /**
     * Contar refresh tokens válidos de un usuario
     * Útil para limitar sesiones concurrentes
     */
    public long countValidRefreshTokensForUser(UUID userId) {
        return refreshTokenRepository.countValidTokensByUserId(userId);
    }
    
    /**
     * Limpiar refresh tokens expirados (para ejecutarse periódicamente)
     */
    @Transactional
    public int deleteExpiredTokens() {
        return refreshTokenRepository.deleteExpiredTokens();
    }
}
