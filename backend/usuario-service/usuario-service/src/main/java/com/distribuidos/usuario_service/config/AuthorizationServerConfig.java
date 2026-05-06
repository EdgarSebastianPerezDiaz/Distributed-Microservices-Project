package com.distribuidos.usuario_service.config;

import com.distribuidos.usuario_service.security.CustomUserDetailsService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * Configuración del Servidor de Autorización OAuth 2.0
 * 
 * Spring Authorization Server 1.2.2 + Spring Boot 3.2.0 + Spring Security 6.2.0
 * 
 * Proporciona:
 * - Endpoints OAuth 2.0: /oauth2/authorize, /oauth2/token, /.well-known/jwks.json
 * - Registro de clientes OAuth (en memoria)
 * - Personalización de tokens JWT con claims personalizados (username, role)
 * - Soporte para Authorization Code, Client Credentials, Refresh Token flows
 * - Emisión de tokens RS256 con claves RSA generadas dinámicamente
 * 
 * Orden: 1 (prioridad más alta que SecurityConfig)
 * 
 * Clientes OAuth 2.0 registrados:
 * 1. frontend-app (client_id=frontend-app)
 *    - Grant: AUTHORIZATION_CODE + REFRESH_TOKEN (para SPA Angular)
 *    - Redirect: http://localhost:4200/auth-callback
 * 
 * 2. microservices-client (client_id=microservices-client)
 *    - Grant: CLIENT_CREDENTIALS + REFRESH_TOKEN (para servicios internos)
 * 
 * Configuración de Tokens:
 * - Access Token: RS256, 1 hora
 * - Refresh Token: 7 días, no reutilizable
 * - Claims: sub, username, role, scope, iss, iat, exp, jti, aud
 */
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

	private final CustomUserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Constructor con inyección de dependencias
	 * El PasswordEncoder viene de SecurityConfig (@Primary)
	 */
	public AuthorizationServerConfig(
		CustomUserDetailsService userDetailsService,
		PasswordEncoder passwordEncoder) {
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Cadena de filtros de seguridad para OAuth 2.0 Authorization Server
	 * Orden: 1 (prioritaria)
	 */
	@Bean
	@Order(1)
	public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		http.exceptionHandling(exceptions ->
			exceptions.authenticationEntryPoint(
				new LoginUrlAuthenticationEntryPoint("/login")
			)
		);
		return http.build();
	}

	/**
	 * Gestor de autenticación global para usuarios
	 */
	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = 
			http.getSharedObject(AuthenticationManagerBuilder.class);
		
		authenticationManagerBuilder
			.userDetailsService(userDetailsService)
			.passwordEncoder(passwordEncoder);
		
		return authenticationManagerBuilder.build();
	}

	/**
	 * Repositorio de clientes OAuth 2.0 registrados en memoria
	 */
	@Bean
	public RegisteredClientRepository registeredClientRepository() {
		// Cliente 1: Frontend Angular (SPA)
		RegisteredClient frontendClient = RegisteredClient
			.withId("1")
			.clientId("frontend-app")
			.clientSecret("frontend-secret-change-me")  // CRÍTICO: cambiar en producción
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
			.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)  // Public client para SPA
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.redirectUri("http://localhost:4200/auth-callback")
			.redirectUri("http://localhost:4200/callback")
			.redirectUri("http://localhost:4200")
			.scope("openid")
			.scope("profile")
			.scope("email")
			.scope("read")
			.scope("write")
			.tokenSettings(tokenSettings())
			.clientSettings(ClientSettings.builder()
				.requireAuthorizationConsent(false)
				.requireProofKey(false)
				.build())
			.build();

		// Cliente 2: Microservicios (Client Credentials)
		RegisteredClient microservicesClient = RegisteredClient
			.withId("2")
			.clientId("microservices-client")
			.clientSecret("microservices-secret-change-me")  // CRÍTICO: cambiar en producción
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.scope("openid")
			.scope("read:services")
			.scope("write:services")
			.tokenSettings(tokenSettings())
			.clientSettings(ClientSettings.builder()
				.requireAuthorizationConsent(false)
				.build())
			.build();

		return new InMemoryRegisteredClientRepository(frontendClient, microservicesClient);
	}

	/**
	 * Servicio de autorización OAuth 2.0 (almacena tokens emitidos)
	 */
	@Bean
	public OAuth2AuthorizationService authorizationService() {
		return new InMemoryOAuth2AuthorizationService();
	}

	/**
	 * Servicio de consentimiento de autorización OAuth 2.0
	 */
	@Bean
	public OAuth2AuthorizationConsentService authorizationConsentService() {
		return new InMemoryOAuth2AuthorizationConsentService();
	}

	/**
	 * Configuración de tokens (duración, algoritmo, refresh, etc.)
	 */
	@Bean
	public TokenSettings tokenSettings() {
		return TokenSettings.builder()
			.accessTokenTimeToLive(Duration.ofHours(1))          // Access token: 1 hora
			.refreshTokenTimeToLive(Duration.ofDays(7))          // Refresh token: 7 días
			.reuseRefreshTokens(false)                           // Generar nuevo refresh token cada uso
			.build();
	}

	/**
	 * Configuración del servidor de autorización
	 * Define endpoints OAuth 2.0 y emisor
	 */
	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder()
			.issuer("http://localhost:8084")                     // CAMBIAR en producción
			.authorizationEndpoint("/oauth2/authorize")
			.tokenEndpoint("/oauth2/token")
			.tokenRevocationEndpoint("/oauth2/revoke")
			.tokenIntrospectionEndpoint("/oauth2/introspect")
			.jwkSetEndpoint("/.well-known/jwks.json")
			.build();
	}

	/**
	 * Generador de claves RSA para RS256
	 * Genera dinámicamente un par de claves RSA-2048 en memoria
	 */
	@Bean
	public JWKSource<SecurityContext> jwkSource() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);  // RSA 2048 bits (estándar OAuth 2.0)
		
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		
		RSAKey rsaKey = new RSAKey.Builder(publicKey)
			.privateKey(privateKey)
			.keyID(UUID.randomUUID().toString())
			.build();
		
		JWKSet jwkSet = new JWKSet(rsaKey);
		return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
	}

	/**
	 * Decodificador JWT para validar tokens emitidos por este servidor
	 */
	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri(
			"http://localhost:8084/.well-known/jwks.json"
		).build();
	}

	/**
	 * Personalizador de tokens JWT
	 * Agrega claims personalizados (username, role) para compatibilidad
	 * con JWT legacy y sistemas cliente
	 */
	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
		return context -> {
			if (context.getTokenType().getValue().equals("access_token")) {
				// Agregar username (compatibilidad con JWT legacy)
				context.getClaims().claim("username", context.getPrincipal().getName());
				
				// Agregar role desde authorities
				if (context.getPrincipal().getAuthorities() != null) {
					var authorities = context.getPrincipal().getAuthorities();
					if (!authorities.isEmpty()) {
						String role = authorities.iterator().next().getAuthority();
						// Remover prefijo ROLE_ si existe
						if (role.startsWith("ROLE_")) {
							role = role.substring(5);
						}
						context.getClaims().claim("role", role);
						context.getClaims().claim("roles", authorities.stream()
							.map(auth -> auth.getAuthority().startsWith("ROLE_") ? 
								auth.getAuthority().substring(5) : auth.getAuthority())
							.toList());
					}
				}
			}
		};
	}

	/**
	 * Convertidor de autenticación JWT para recursos OAuth 2.0
	 * Convierte JWT en objetos Authentication
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("scope");
		authoritiesConverter.setAuthorityPrefix("SCOPE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}
}
