package com.distribuidos.usuario_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Validador dual de tokens JWT
 * 
 * Soporta dos tipos:
 * 1. JWT Legacy (HS512 con clave compartida) - Sistema actual
 * 2. OAuth 2.0 JWT (RS256 con claves RSA) - Nuevo sistema
 * 
 * Permite coexistencia de ambos sistemas durante transición
 */
@Component
public class DualJwtValidator {

	// CLAVE SECRETA LEGACY: Misma del sistema actual
	private static final String LEGACY_SECRET_KEY = 
		"mi-clave-super-secreta-para-jwt-de-512-bits-minimo-requerido-para-firmar-tokens-seguros-en-el-sistema-de-contratos-uptc-2026";

	private final SecretKey legacyKey;

	public DualJwtValidator() {
		// Inicializar clave para JWT legacy
		this.legacyKey = Keys.hmacShaKeyFor(LEGACY_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Convierte un token JWT (legacy u OAuth) a Authentication
	 * 
	 * Intenta validar como JWT legacy primero, luego como OAuth 2.0
	 */
	public Authentication validateAndConvertToken(String token) {
		// Primero intenta como JWT legacy
		try {
			Claims claims = validateLegacyJwt(token);
			return createAuthenticationFromLegacyJwt(claims);
		} catch (Exception e) {
			// Si falla, se espera que OAuth 2.0 lo procese
			throw new RuntimeException("Token inválido: " + e.getMessage());
		}
	}

	/**
	 * Valida un token JWT legacy (HS512)
	 */
	public Claims validateLegacyJwt(String token) {
		try {
			return Jwts.parserBuilder()
				.setSigningKey(legacyKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
		} catch (Exception e) {
			throw new RuntimeException("Token JWT legacy inválido: " + e.getMessage(), e);
		}
	}

	/**
	 * Crea una Authentication desde claims de JWT legacy
	 */
	private Authentication createAuthenticationFromLegacyJwt(Claims claims) {
		String username = (String) claims.get("username");
		String role = (String) claims.get("role");
		
		SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
		
		return new JwtAuthenticationToken(
			Jwt.withTokenValue("legacy")
				.header("alg", "HS512")
				.subject(claims.getSubject())
				.claim("username", username)
				.claim("role", role)
				.issuedAt(claims.getIssuedAt().toInstant())
				.expiresAt(claims.getExpiration().toInstant())
				.build(),
			Collections.singleton(authority),
			username
		);
	}

	/**
	 * Crea una Authentication desde JWT de OAuth 2.0
	 */
	public Authentication createAuthenticationFromOAuth2Jwt(Jwt jwt) {
		String username = jwt.getClaimAsString("username");
		String role = jwt.getClaimAsString("role");
		
		SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
		
		return new JwtAuthenticationToken(jwt, Collections.singleton(authority), username);
	}

	/**
	 * Verifica si un token es JWT legacy (HS512)
	 */
	public boolean isLegacyJwt(String token) {
		try {
			// Decodificar sin validar firma (solo verificar estructura)
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(legacyKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
			
			// Si llega aquí, es un JWT legacy válido
			return claims.get("username") != null && claims.get("role") != null;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Extrae el UUID del usuario desde claims legacy
	 */
	public String extractUserIdFromLegacy(Claims claims) {
		return claims.getSubject();
	}

	/**
	 * Extrae el username desde claims legacy
	 */
	public String extractUsernameFromLegacy(Claims claims) {
		return (String) claims.get("username");
	}

	/**
	 * Extrae el role desde claims legacy
	 */
	public String extractRoleFromLegacy(Claims claims) {
		return (String) claims.get("role");
	}
}
