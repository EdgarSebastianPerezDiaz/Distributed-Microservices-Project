package com.distribuidos.usuario_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador que proporciona información sobre OAuth 2.0
 * Útil para clientes que deseen usar el nuevo sistema de autorización
 */
@RestController
@RequestMapping("/api/oauth2-info")
public class OAuth2InfoController {

	@Value("${oauth2.issuer-uri:http://localhost:8084}")
	private String issuerUri;

	/**
	 * Información sobre endpoints disponibles
	 */
	@GetMapping("/endpoints")
	public ResponseEntity<Map<String, Object>> getOAuthEndpoints() {
		Map<String, Object> endpoints = new HashMap<>();
		
		endpoints.put("token_endpoint", issuerUri + "/oauth2/token");
		endpoints.put("authorization_endpoint", issuerUri + "/oauth2/authorize");
		endpoints.put("revocation_endpoint", issuerUri + "/oauth2/revoke");
		endpoints.put("jwks_endpoint", issuerUri + "/oauth2/jwks");
		endpoints.put("well_known_config", issuerUri + "/.well-known/oauth-authorization-server");
		
		Map<String, Object> response = new HashMap<>();
		response.put("issuer", issuerUri);
		response.put("endpoints", endpoints);
		
		return ResponseEntity.ok(response);
	}

	/**
	 * Información sobre clientes registrados
	 * Solo para referencia (no incluye secrets)
	 */
	@GetMapping("/clients")
	public ResponseEntity<Map<String, Object>> getRegisteredClients() {
		Map<String, Object> clients = new HashMap<>();
		
		// Frontend Client
		Map<String, Object> frontendClient = new HashMap<>();
		frontendClient.put("client_id", "frontend-app");
		frontendClient.put("grant_types", new String[]{"authorization_code", "refresh_token"});
		frontendClient.put("scopes", new String[]{"openid", "profile", "users.read", "users.write"});
		frontendClient.put("redirect_uris", new String[]{
			"http://localhost:4200/callback",
			"http://localhost:4200/login/callback"
		});
		frontendClient.put("flow", "Authorization Code");
		
		// Microservices Client
		Map<String, Object> microservicesClient = new HashMap<>();
		microservicesClient.put("client_id", "microservices-client");
		microservicesClient.put("grant_types", new String[]{"client_credentials", "refresh_token"});
		microservicesClient.put("scopes", new String[]{"users.read", "users.admin"});
		microservicesClient.put("flow", "Client Credentials");
		
		clients.put("frontend", frontendClient);
		clients.put("microservices", microservicesClient);
		
		Map<String, Object> response = new HashMap<>();
		response.put("clients", clients);
		response.put("note", "Para obtener el client_secret, contactar al administrador");
		
		return ResponseEntity.ok(response);
	}

	/**
	 * Ejemplo de cómo obtener un token para microservicios
	 */
	@GetMapping("/example-client-credentials")
	public ResponseEntity<Map<String, Object>> getClientCredentialsExample() {
		Map<String, Object> example = new HashMap<>();
		
		Map<String, String> request = new HashMap<>();
		request.put("method", "POST");
		request.put("url", issuerUri + "/oauth2/token");
		request.put("content_type", "application/x-www-form-urlencoded");
		
		Map<String, String> body = new HashMap<>();
		body.put("grant_type", "client_credentials");
		body.put("client_id", "microservices-client");
		body.put("client_secret", "microservices-secret");
		body.put("scope", "users.read");
		
		Map<String, Object> response_example = new HashMap<>();
		response_example.put("access_token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...");
		response_example.put("token_type", "Bearer");
		response_example.put("expires_in", 3600);
		response_example.put("scope", "users.read");
		
		example.put("request", request);
		example.put("body", body);
		example.put("response_example", response_example);
		
		Map<String, Object> result = new HashMap<>();
		result.put("example", example);
		result.put("note", "Usar este token en header: Authorization: Bearer <access_token>");
		
		return ResponseEntity.ok(result);
	}

	/**
	 * Información de migración del JWT Legacy a OAuth 2.0
	 */
	@GetMapping("/migration-info")
	public ResponseEntity<Map<String, Object>> getMigrationInfo() {
		Map<String, Object> info = new HashMap<>();
		
		Map<String, String> legacy = new HashMap<>();
		legacy.put("endpoint", "/api/auth/login");
		legacy.put("method", "POST");
		legacy.put("status", "FUNCIONANDO (Compatible hacia atrás)");
		legacy.put("token_type", "JWT HS512");
		legacy.put("token_lifetime", "24 horas");
		
		Map<String, String> oauth2 = new HashMap<>();
		oauth2.put("endpoint", "/oauth2/token");
		oauth2.put("method", "POST");
		oauth2.put("status", "NUEVO (Recomendado para nuevos clientes)");
		oauth2.put("token_type", "JWT RS256");
		oauth2.put("token_lifetime", "1 hora (con refresh token)");
		oauth2.put("security_level", "SUPERIOR (Refresh tokens, RSA keys, tokens cortos)");
		
		Map<String, Object> comparison = new HashMap<>();
		comparison.put("legacy_jwt", legacy);
		comparison.put("oauth2_jwt", oauth2);
		
		info.put("comparison", comparison);
		info.put("recommendation", "Migrar nuevos clientes a OAuth 2.0");
		info.put("timeline", "Fase 1: Ambos funcionan | Fase 2: Deprecar JWT Legacy");
		
		return ResponseEntity.ok(info);
	}
}
