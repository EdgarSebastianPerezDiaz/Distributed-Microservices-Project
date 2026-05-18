# 🏗️ Arquitectura Técnica - OAuth 2.0 Dual Authentication

## Vista General del Sistema

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENTES EXTERNOS                           │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐   │
│  │ Browser SPA  │  │ Mobile App   │  │ Microservicios        │   │
│  │ (Angular)    │  │ (iOS/Android)│  │ (Cliente-Cliente)     │   │
│  └──────┬───────┘  └──────┬───────┘  └────────┬───────────────┘   │
└─────────┼──────────────────┼────────────────────┼───────────────────┘
          │                  │                    │
          └──────────────────┼────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  API Gateway    │
                    │   (puerto 8081) │
                    │ Spring Cloud    │
                    │    Gateway      │
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
    ┌─────▼─────┐    ┌──────▼─────┐    ┌──────▼──────┐
    │ JWT Legacy│    │  OAuth 2.0 │    │ Other Routes│
    │  (HS512)  │    │  (RS256)   │    │ (Contracts, │
    │  24 horas │    │  1 hora    │    │  Suppliers) │
    │  HS512 Key│    │  RSA-2048  │    │             │
    └─────┬─────┘    └──────┬─────┘    └──────┬──────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
                  ┌─────────▼──────────┐
                  │ Usuario Service    │
                  │  (puerto 8084)     │
                  │  Spring Boot 3.2   │
                  │  + Spring Security │
                  │  + Spring AuthZ    │
                  │     Server         │
                  └─────────┬──────────┘
                            │
        ┌───────────────────┼────────────────────┐
        │                   │                    │
   ┌────▼────┐        ┌─────▼─────┐      ┌──────▼──────┐
   │PostgreSQL│       │  Eureka   │      │  Security   │
   │   BD     │       │  Server   │      │  Context    │
   │ (BD)     │       │(8761)     │      │  (Memory)   │
   └─────────┘       └───────────┘      └─────────────┘
```

---

## Componentes de Seguridad Detallados

### SecurityConfig Chain (@Order 2)

```
┌─────────────────────────────────────────────────────────────┐
│                    HttpSecurity Chain                       │
│                                                             │
│  1. CORS Configuration                                     │
│  2. CSRF Disabled (Stateless API)                         │
│  3. Session Policy: STATELESS                             │
│  4. Authorization Rules:                                  │
│     ├─ OPTIONS /* → permitAll                            │
│     ├─ POST /api/auth/login → permitAll                 │
│     ├─ POST /api/auth/register → permitAll              │
│     ├─ /oauth2/** → permitAll ✨ NUEVO                  │
│     ├─ /.well-known/** → permitAll ✨ NUEVO             │
│     ├─ /actuator/** → permitAll                         │
│     └─ /* → AUTHENTICATED                               │
│  5. Add Filter: JwtAuthenticationFilter                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### JwtAuthenticationFilter Flow (Dual Mode)

```
Request con Authorization Header
    │
    ├─ ¿Es ruta pública (/actuator, /oauth2, etc.)?
    │  ├─ SÍ → Pasar directo (doFilter)
    │  └─ NO → Continuar ↓
    │
    ├─ ¿Tiene Authorization header?
    │  ├─ NO → Passar a siguiente filtro (OAuth2 lo procesará)
    │  └─ SÍ → Continuar ↓
    │
    ├─ Extraer token de "Bearer {token}"
    │
    ├─ ¿Es JWT Legacy (HS512)?
    │  ├─ SÍ → DualJwtValidator.validateLegacyJwt()
    │  │       ├─ ¿Válido?
    │  │       │  ├─ SÍ → Crear Authentication Legacy
    │  │       │  │       SetSecurityContext()
    │  │       │  │       Pasar al siguiente filtro ✅
    │  │       │  └─ NO → Clearcontext, Log, Continuar ❌
    │  └─ NO → Es OAuth2 (RS256)
    │          └─ Dejar que OAuth2 Resource Server lo procese
    │
    └─ Pasar al siguiente filtro en cadena
```

### AuthorizationServerConfig (@Order 1)

```
┌───────────────────────────────────────────────────────────────┐
│              OAuth 2.0 Authorization Server                   │
│                                                               │
│  Precedencia: @Order(1) > SecurityConfig @Order(2)           │
│                                                               │
│  Proporciona Endpoints:                                      │
│  ├─ POST /oauth2/token ..................... Token Endpoint  │
│  ├─ GET /oauth2/authorize .................. Auth Endpoint   │
│  ├─ POST /oauth2/revoke .................... Revoke Endpoint │
│  └─ GET /.well-known/jwks.json ............ JWKS Endpoint   │
│                                                               │
│  Clientes Registrados:                                       │
│  ├─ frontend-app                                            │
│  │  ├─ Grant Types: authorization_code, refresh_token      │
│  │  ├─ Scopes: openid, profile, email                      │
│  │  └─ RedirectURIs: http://localhost:4200/**              │
│  │                                                           │
│  └─ microservices-client                                    │
│     ├─ Grant Types: client_credentials, refresh_token      │
│     ├─ Scopes: read, write                                 │
│     └─ No redirect (Server-to-server)                      │
│                                                               │
│  Token Settings:                                            │
│  ├─ Access Token: 1 hora (RS256)                            │
│  ├─ Refresh Token: 7 días (no reutilizable)               │
│  └─ Algoritmo: RS256 (RSA-2048)                            │
│                                                               │
│  Key Generation:                                            │
│  └─ RSA KeyPair 2048-bit: Generado dinámicamente            │
│                                                               │
│  Token Customization:                                       │
│  └─ Agrega claims: username, role, roles                   │
│                                                               │
│  Servicios:                                                 │
│  ├─ OAuth2AuthorizationService (in-memory)                │
│  ├─ OAuth2AuthorizationConsentService (in-memory)         │
│  └─ RegisteredClientRepository (in-memory)                │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

---

## Flujo de Autenticación: Comparación

### Flujo 1: JWT Legacy (Actual)

```
┌─────────┐
│ Cliente │
└────┬────┘
     │
     │ 1. POST /api/auth/login
     │    {username: "admin", password: "Admin123"}
     │
     ▼
┌─────────────────────────┐
│ API Gateway (8081)      │
│ ├─ JwtAuthFilter       │
│ └─ Pasar a usuario-svc │
└────┬────────────────────┘
     │
     │ 2. POST /api/auth/login
     │
     ▼
┌──────────────────────────────────┐
│ Usuario Service (8084)           │
│ ├─ AuthController.login()        │
│ ├─ Validar credenciales          │
│ ├─ Generar JWT HS512 (24h)      │
│ └─ Retornar token                │
└────┬─────────────────────────────┘
     │
     │ 3. Response con token JWT HS512
     │    {token: "eyJhbGciOiJIUzUxMi..."}
     │
     ▼
┌─────────┐
│ Cliente │ ← Guardar token por 24 horas
└────┬────┘
     │
     │ 4. GET /api/auth/users
     │    Authorization: Bearer eyJhbGciOiJIUzUxMi...
     │
     ▼
┌─────────────────────────┐
│ API Gateway (8081)      │
│ ├─ JwtAuthFilter        │
│ ├─ DualJwtValidator     │
│ ├─ isLegacyJwt() = true │
│ ├─ validateLegacyJwt()  │
│ ├─ ✅ Válido            │
│ └─ Pasar al siguiente   │
└────┬────────────────────┘
     │
     ▼
┌──────────────────────────────────┐
│ Usuario Service (8084)           │
│ ├─ AuthController.getUsers()     │
│ └─ Retornar lista de usuarios    │
└────┬─────────────────────────────┘
     │
     │ 5. Response: [lista usuarios]
     │
     ▼
┌─────────┐
│ Cliente │ ← Token sigue siendo válido 24h
└─────────┘
```

### Flujo 2: OAuth 2.0 Client Credentials (Nuevo)

```
┌──────────────────────┐
│ Microservicio Cliente │
│ (proveedor-service)  │
└────┬─────────────────┘
     │
     │ 1. POST /oauth2/token
     │    {grant_type: "client_credentials",
     │     client_id: "microservices-client",
     │     client_secret: "...",
     │     scope: "read"}
     │
     ▼
┌──────────────────────────┐
│ API Gateway (8081)       │
│ ├─ JwtAuthFilter         │
│ ├─ ¿ruta /oauth2?        │
│ ├─ YES → permitAll       │
│ └─ Pasar a usuario-svc   │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────────────────┐
│ Usuario Service (8084)               │
│ ├─ AuthorizationServerConfig         │
│ │  (@Order 1 - Prioridad máxima)    │
│ │                                    │
│ ├─ 1. Validar client credentials    │
│ │    ├─ client_id = "microservices" │
│ │    ├─ client_secret = válido ✅   │
│ │    └─ Grant = client_credentials  │
│ │                                    │
│ ├─ 2. Generar tokens (RS256)        │
│ │    ├─ Access Token: 1 hora        │
│ │    ├─ Claims: {sub, username,     │
│ │    │           role, scope, iss,  │
│ │    │           iat, exp, jti}     │
│ │    └─ Refresh Token: 7 días       │
│ │                                    │
│ └─ 3. Guardar refresh_token en BD   │
│    (tabla: oauth_refresh_tokens)    │
└────┬───────────────────────────────┘
     │
     │ 2. Response:
     │    {access_token: "eyJhbGciOiJSUzI1Ni...",
     │     refresh_token: "...",
     │     token_type: "Bearer",
     │     expires_in: 3600,
     │     scope: "read"}
     │
     ▼
┌──────────────────────┐
│ Microservicio Cliente │ ← Guardar tokens
└────┬─────────────────┘
     │
     │ 3. GET /api/auth/users
     │    Authorization: Bearer eyJhbGciOiJSUzI1Ni...
     │
     ▼
┌──────────────────────────────┐
│ API Gateway (8081)           │
│ ├─ JwtAuthFilter             │
│ ├─ DualJwtValidator           │
│ ├─ isLegacyJwt() = false      │
│ ├─ → OAuth2 Resource Server   │
│ └─ JwtDecoder valida RS256    │
└────┬─────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ Usuario Service (8084)         │
│ ├─ OAuth2ResourceServer        │
│ ├─ JwtAuthenticationConverter  │
│ ├─ Validar claims             │
│ ├─ Crear Authentication       │
│ └─ Pasar a AuthController     │
└────┬───────────────────────────┘
     │
     ▼
┌────────────────────────────────┐
│ AuthController.getUsers()      │
│ └─ Retornar lista usuarios     │
└────┬───────────────────────────┘
     │
     ▼
┌──────────────────┐
│ Response: [...]  │
└──────────────────┘

                    ↓
        ┌─── 1 HORA DESPUÉS ───┐
        │ Access token expira  │
        └─────────┬────────────┘
                  │
     │ 4. POST /oauth2/token
     │    {grant_type: "refresh_token",
     │     refresh_token: "...",
     │     client_id: "...",
     │     client_secret: "..."}
     │
     ▼
┌────────────────────────────────┐
│ Usuario Service (8084)         │
│ ├─ AuthorizationServerConfig   │
│ │                              │
│ ├─ 1. Validar refresh token   │
│ │    └─ ¿Existe en BD?         │
│ │    └─ ¿No expirado?          │
│ │    └─ ¿No revocado?          │
│ │                              │
│ ├─ 2. Generar nuevos tokens   │
│ │    ├─ Nuevo access token     │
│ │    ├─ Nuevo refresh token    │
│ │    └─ Guardar en BD          │
│ │                              │
│ └─ 3. Retornar nuevos tokens  │
└────┬───────────────────────────┘
     │
     ▼
┌──────────────────────┐
│ Microservicio Cliente │ ← Nuevos tokens
└──────────────────────┘
```

---

## Estructura de Tablas PostgreSQL

### oauth_refresh_tokens

```
┌─────────────────────────────────────────────────────────────┐
│              oauth_refresh_tokens TABLE                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ id (UUID) ........................... Primary Key         │
│ token_value (TEXT) .................. UNIQUE, NOT NULL   │
│ user_id (UUID) ...................... FK usuarios(id)    │
│ client_id (VARCHAR 100) ............. NOT NULL           │
│ expires_at (TIMESTAMP) .............. NOT NULL           │
│ revoked (BOOLEAN) ................... DEFAULT FALSE      │
│ created_at (TIMESTAMP) .............. DEFAULT NOW()     │
│ updated_at (TIMESTAMP) .............. DEFAULT NOW()     │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                          INDICES                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ idx_oauth_refresh_tokens_token_value                      │
│   └─ Búsqueda rápida por token                           │
│                                                             │
│ idx_oauth_refresh_tokens_user_id                          │
│   └─ Búsqueda por usuario                                │
│                                                             │
│ idx_oauth_refresh_tokens_expires_at                       │
│   └─ Búsqueda de tokens expirados (limpieza)             │
│                                                             │
│ idx_oauth_refresh_tokens_client_id                        │
│   └─ Búsqueda por cliente OAuth                          │
│                                                             │
│ idx_oauth_refresh_tokens_active                           │
│   └─ Composite: (user_id, revoked, expires_at)           │
│      Búsqueda de tokens activos por usuario              │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                       VISTAS (VIEWS)                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ vw_active_oauth_tokens                                   │
│   └─ Tokens activos: revoked=false AND expires_at>now   │
│                                                             │
│ vw_oauth_tokens_history                                  │
│   └─ Historial completo con estados (ACTIVE, EXPIRED,    │
│      REVOKED)                                            │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                     TRIGGERS                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ trigger_oauth_refresh_tokens_updated_at                  │
│   └─ Actualiza updated_at automáticamente antes de        │
│      cualquier UPDATE                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Flujo de Spring Security

```
┌──────────────────────────────────────────────────────────┐
│                    INCOMING REQUEST                      │
└─────────────────────────┬────────────────────────────────┘
                          │
         ┌────────────────┴────────────────┐
         │                                 │
         ▼                                 ▼
    ┌─────────────────┐          ┌──────────────────────┐
    │ Async Wrapped   │          │ Spring Security      │
    │ Filter Proxy    │          │ Filter Chain         │
    └────────┬────────┘          └──────────┬───────────┘
             │                              │
             └──────────────┬───────────────┘
                            │
              ┌─────────────▼──────────────┐
              │  AuthorizationServerConfig │
              │  SecurityFilterChain       │
              │  (@Order 1)                │
              │                            │
              │  OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)
              │  ├─ OAuth2TokenEndpointFilter
              │  ├─ OAuth2ClientAuthentication
              │  ├─ AuthorizationCodeRequestAuthenticationFilter
              │  └─ ...más filtros OAuth2
              │                            │
              └────────────┬───────────────┘
                           │
               ┌───────────▼────────────┐
               │  SecurityConfig        │
               │  SecurityFilterChain   │
               │  (@Order 2)            │
               │                        │
               │  ├─ CorsFilter         │
               │  ├─ CsrfFilter (disabled for API)
               │  ├─ SecurityContextPersistenceFilter
               │  ├─ LogoutFilter
               │  ├─ AuthenticationProcessingFilter
               │  ├─ FormLoginFilter
               │  ├─ OAuth2LoginAuthenticationFilter
               │  ├─ OAuth2ResourceServerFilter
               │  ├─ JwtAuthenticationFilter ← CUSTOM
               │  │   │
               │  │   ├─ Verificar Authorization header
               │  │   ├─ Extraer token
               │  │   ├─ DualJwtValidator.isLegacyJwt()?
               │  │   │  ├─ SÍ: validateLegacyJwt() + createAuth
               │  │   │  └─ NO: Pasar a OAuth2 (siguiente filtro)
               │  │   │
               │  │   └─ SetSecurityContext(Authentication)
               │  │
               │  ├─ ExceptionTranslationFilter
               │  ├─ FilterSecurityInterceptor
               │  └─ ...más filtros
               │
               └────────────┬───────────┘
                            │
                   ┌────────▼────────┐
                   │ DISPATCHER       │
                   │ (al endpoint)    │
                   └────────┬────────┘
                            │
                   ┌────────▼────────┐
                   │ RESPONSE        │
                   │ (al cliente)    │
                   └─────────────────┘
```

---

## Mapeo de Rutas en API Gateway

```
Client Request
    │
    ├─ Path Predicate Match?
    │
    ├─ POST /api/auth/login
    │  └─ Route: usuario-service
    │     ├─ URI: lb://usuario-service
    │     └─ Pasar a SecurityConfig/AuthController.login()
    │
    ├─ POST /oauth2/token ✨ NUEVO
    │  └─ Route: usuario-service
    │     ├─ URI: lb://usuario-service
    │     ├─ Public route (permitAll)
    │     └─ Pasar a AuthorizationServerConfig/TokenEndpoint
    │
    ├─ GET /.well-known/jwks.json ✨ NUEVO
    │  └─ Route: usuario-service
    │     ├─ URI: lb://usuario-service
    │     ├─ Public route (permitAll)
    │     └─ Pasar a OAuth2 JWKS Endpoint
    │
    ├─ GET /api/oauth2-info/** ✨ NUEVO
    │  └─ Route: usuario-service
    │     ├─ URI: lb://usuario-service
    │     ├─ Public route (permitAll)
    │     └─ Pasar a OAuth2InfoController
    │
    ├─ GET /api/auth/users (Requiere token)
    │  └─ Route: usuario-service
    │     ├─ URI: lb://usuario-service
    │     ├─ JwtAuthenticationFilter valida token
    │     └─ Pasar a AuthController.getUsers()
    │
    ├─ GET /api/suppliers/**
    │  └─ Route: proveedor-service
    │
    ├─ GET /api/contracts/**
    │  └─ Route: contrato-service
    │
    └─ GET /api/audit/**
       └─ Route: audit-service
```

---

## Comparativa: JWT Legacy vs OAuth 2.0

```
┌──────────────────┬──────────────────────┬──────────────────────┐
│ Característica   │ JWT Legacy (HS512)   │ OAuth 2.0 (RS256)    │
├──────────────────┼──────────────────────┼──────────────────────┤
│ Algoritmo        │ HS512 (HMAC+SHA512)  │ RS256 (RSA+SHA256)   │
│ Clave Privada    │ Clave compartida     │ Private Key (servidor)
│ Clave Pública    │ Clave compartida     │ Public JWKSet        │
│ Seguridad Firma  │ Media (simetría)     │ Alta (asimetría)     │
│ Duración Token   │ 24 horas             │ 1 hora               │
│ Refresh Token    │ No soportado         │ Soportado (7 días)   │
│ Revocación       │ Difícil              │ Fácil (BD)           │
│ Almacenamiento   │ No (stateless)       │ oauth_refresh_tokens │
│ Escalabilidad    │ Media (clave shared) │ Alta (public keys)   │
│ Standards        │ Propietario          │ IETF RFC 6749        │
│ Migración BD     │ No necesario         │ oauth2_schema.sql    │
│ Rotación Keys    │ No                   │ Soportado            │
└──────────────────┴──────────────────────┴──────────────────────┘
```

---

**Diagrama creado**: Mayo 2026  
**Versión**: 1.0  
**Rama**: feature/oauth2-dual
