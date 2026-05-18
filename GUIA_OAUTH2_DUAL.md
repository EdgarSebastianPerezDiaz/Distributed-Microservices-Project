# 🔐 Guía de Implementación: OAuth 2.0 Dual Authentication

## 📋 Tabla de Contenidos

1. [Descripción General](#descripción-general)
2. [Arquitectura](#arquitectura)
3. [Requisitos Previos](#requisitos-previos)
4. [Instalación y Configuración](#instalación-y-configuración)
5. [Ejecución de Servicios](#ejecución-de-servicios)
6. [Pruebas](#pruebas)
7. [Endpoints Disponibles](#endpoints-disponibles)
8. [Flujos de Autenticación](#flujos-de-autenticación)
9. [Troubleshooting](#troubleshooting)
10. [Migración Futura](#migración-futura)

---

## 🎯 Descripción General

### Objetivo
Implementar **OAuth 2.0** usando **Spring Authorization Server** en el microservicio `usuario-service` **sin eliminar** el sistema actual de JWT Legacy (HS512), permitiendo que ambos sistemas coexistan durante un período de transición.

### Contexto
- **Rama**: `feature/oauth2-dual`
- **Proyecto**: Distributed Microservices for Public Contracts Management
- **Sistema Actual**: JWT Legacy con HS512, tokens de 24 horas
- **Nuevo Sistema**: OAuth 2.0 con RS256, tokens de 1 hora + refresh tokens de 7 días

### Beneficios de OAuth 2.0
| Aspecto | JWT Legacy | OAuth 2.0 |
|--------|-----------|----------|
| **Algoritmo** | HS512 (HMAC) | RS256 (RSA) |
| **Validación** | Clave compartida | Claves públicas |
| **Duración Token** | 24 horas | 1 hora |
| **Refresh Token** | ❌ No | ✅ Sí (7 días) |
| **Revocación** | ❌ Difícil | ✅ Fácil |
| **Estándar** | Propietario | IETF RFC 6749 |
| **Seguridad** | Media | Alta |
| **Escalabilidad** | Media | Alta |

---

## 🏗️ Arquitectura

### Diagrama de Flujo General

```
┌─────────────────────────────────────────────────────────────┐
│                   Cliente Externo (Browser/App)             │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ↓
                    ┌────────────────────┐
                    │  API Gateway (8081)│
                    └────────┬───────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
     ┌─────▼────┐   ┌────────▼──────┐   ┌──────▼──────┐
     │Legacy JWT │   │ OAuth 2.0 JWT │   │Other Routes │
     │HS512      │   │ RS256         │   │(Contracts,  │
     │24h tokens │   │1h tokens      │   │ Suppliers)  │
     └─────┬────┘   └────────┬──────┘   └──────┬──────┘
           │                 │                 │
           │                 │                 │
           └────────────┬────┴────────┬────────┘
                        │             │
                ┌───────▼──────┬──────▼────────┐
                │   Usuario    │Other Services │
                │   Service    │(proveedor,    │
                │   (8084)     │contratos, etc)│
                └──────────────┴───────────────┘
```

### Componentes Principales en usuario-service

```
usuario-service (Spring Boot 3.2 + Spring Security 6.2)
│
├─ Security Configuration
│  ├─ AuthorizationServerConfig (@Order 1)
│  │  ├─ OAuth 2.0 Authorization Server
│  │  ├─ Registered Clients (frontend-app, microservices-client)
│  │  ├─ Token Settings (1h access, 7d refresh)
│  │  └─ RSA-2048 Key Generation
│  │
│  └─ SecurityConfig (@Order 2)
│     ├─ JwtAuthenticationFilter (Legacy + OAuth2)
│     ├─ DualJwtValidator
│     └─ CustomUserDetailsService
│
├─ Models
│  ├─ User (entidad existente)
│  └─ OAuthRefreshToken (nueva)
│
├─ Repositories
│  ├─ UserRepository (existente)
│  └─ OAuthRefreshTokenRepository (nueva)
│
├─ Services
│  ├─ JwtService (Legacy)
│  └─ OAuth2RefreshTokenService (nueva)
│
├─ Controllers
│  ├─ AuthController (existente, /api/auth/login)
│  └─ OAuth2InfoController (nueva, /api/oauth2-info/**)
│
└─ Config
   ├─ application.yaml
   └─ oauth2_schema.sql (BD)
```

---

## 📦 Requisitos Previos

### Software Obligatorio

```bash
# Verificar instalación
java -version          # Java 17+
mvn -v                 # Maven 3.9+
psql --version         # PostgreSQL 12+
docker --version       # Docker (opcional, para BD)
curl --version         # cURL para pruebas
```

### Versiones Específicas

- **Java**: 17 (OpenJDK)
- **Spring Boot**: 3.2.0
- **Spring Cloud**: 2023.0.0
- **Spring Authorization Server**: 1.2.1 (automático en pom.xml)
- **PostgreSQL**: 12+
- **Maven**: 3.9+

### Puertos Requeridos

| Servicio | Puerto | Estado |
|----------|--------|---------|
| PostgreSQL | 5432 | Debe estar disponible |
| Eureka Server | 8761 | Debe estar ejecutando |
| API Gateway | 8081 | Debe estar ejecutando |
| usuario-service | 8084 | Será ejecutado |
| proveedor-service | 8082 | Opcional |
| contrato-service | 8083 | Opcional |

---

## 🛠️ Instalación y Configuración

### Paso 1: Verificar la Rama de Desarrollo

```bash
cd Distributed-Microservices-Project
git status
git branch -a

# Verificar que está en develop (o la rama principal)
# Si no, cambiar a develop
git checkout develop
```

### Paso 2: Crear Rama feature/oauth2-dual

```bash
# Crear rama nueva a partir de develop
git checkout -b feature/oauth2-dual

# Verificar que está en la rama correcta
git branch
# Output esperado: * feature/oauth2-dual
```

### Paso 3: Preparar la Base de Datos

#### Opción A: PostgreSQL Local

```bash
# 1. Asegurarse que PostgreSQL está corriendo
# En Windows:
net start postgresql-x64-15

# 2. Conectarse a PostgreSQL
psql -U postgres

# 3. Crear la BD (si no existe)
CREATE DATABASE usuarios_db;

# 4. Aplicar el esquema OAuth 2.0
\c usuarios_db
\i 'C:/ruta/a/backend/usuario-service/oauth2_schema.sql'

# 5. Verificar creación de tabla
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' AND table_name = 'oauth_refresh_tokens';
# Output esperado: oauth_refresh_tokens
```

#### Opción B: Docker (Recomendado)

```bash
# Crear contenedor PostgreSQL con usuario-service DB
docker run --name postgres-usuarios \
  -e POSTGRES_PASSWORD=Admin123 \
  -e POSTGRES_DB=usuarios_db \
  -p 5432:5432 \
  -d postgres:15

# Esperar 5 segundos y aplicar esquema
sleep 5
docker exec -i postgres-usuarios psql -U postgres -d usuarios_db \
  < backend/usuario-service/oauth2_schema.sql
```

### Paso 4: Compilar usuario-service

```bash
cd backend/usuario-service/usuario-service

# Limpiar builds anteriores
mvn clean

# Compilar con dependencias de OAuth 2.0
mvn compile

# Crear JAR ejecutable
mvn package -DskipTests
# Debería generar: target/usuario-service-0.0.1-SNAPSHOT.jar
```

### Paso 5: Compilar API Gateway

```bash
cd ../../api-gateway/api-gateway

# Compilar y empaquetar
mvn clean compile package -DskipTests
# Debería generar: target/api-gateway-0.0.1-SNAPSHOT.jar
```

---

## 🚀 Ejecución de Servicios

### Orden de Inicio (CRÍTICO)

> ⚠️ **IMPORTANTE**: Iniciar en este orden específico para evitar errores

#### 1. Eureka Server (Terminal 1)

```bash
cd backend/eureka-server/eureka-server

# Opción A: Maven
mvn spring-boot:run

# Opción B: JAR directo
java -jar target/eureka-server-0.0.1-SNAPSHOT.jar

# Verificar: http://localhost:8761/eureka/
# Debería mostrar Dashboard sin servicios aún
```

#### 2. Usuario Service (Terminal 2)

```bash
cd backend/usuario-service/usuario-service

# Opción A: Maven
mvn spring-boot:run

# Opción B: JAR directo
java -jar target/usuario-service-0.0.1-SNAPSHOT.jar

# Esperar logs:
# INFO : Started UsuarioServiceApplication in X.XXX seconds
# INFO : Registered instance USUARIO-SERVICE with status UP
```

#### 3. API Gateway (Terminal 3)

```bash
cd backend/api-gateway/api-gateway

# Opción A: Maven
mvn spring-boot:run

# Opción B: JAR directo
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar

# Esperar logs:
# INFO : Started ApiGatewayApplication in X.XXX seconds
# INFO : Netty started with worker group
```

#### 4. Otros Servicios (Opcional)

```bash
# Si necesitas proveedor-service, contrato-service, etc.
# Seguir el mismo patrón en Terminales adicionales
```

### Verificación de Startup

```bash
# Terminal adicional - Verificar que todos los servicios estén disponibles

# Eureka Dashboard
curl http://localhost:8761/eureka/apps

# Gateway Health
curl http://localhost:8081/actuator/health

# Usuario Service Health
curl http://localhost:8084/actuator/health

# Debería devolver HTTP 200 con status UP en todos
```

---

## 🧪 Pruebas

### Método 1: Script Bash Automatizado (Recomendado)

```bash
# Dar permisos de ejecución
chmod +x test-oauth.sh

# Ejecutar pruebas
./test-oauth.sh

# Output esperado:
# ✓ Pruebas pasadas: 10
# ✓ TODAS LAS PRUEBAS PASARON
# OAuth 2.0 Dual Authentication está funcionando correctamente
```

### Método 2: Postman Collection

```bash
# Importar en Postman
1. Abrir Postman
2. Importar: OAuth2-Dual-Tests.postman_collection.json
3. Configurar variables:
   - {{gateway_url}}: http://localhost:8081
   - {{usuario_service_url}}: http://localhost:8084
4. Ejecutar colección (Run Collection)
```

### Método 3: Pruebas Manuales con cURL

#### Login Legacy

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin123"
  }'

# Respuesta esperada:
# {
#   "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
#   "tokenType": "Bearer",
#   "userId": "550e8400-e29b-41d4-a716-446655440000"
# }
```

#### Usar Token Legacy

```bash
# Guardar token de la respuesta anterior
TOKEN="eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."

curl -X GET http://localhost:8081/api/auth/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

# Respuesta esperada: Array JSON de usuarios
```

#### OAuth 2.0 Client Credentials

```bash
curl -X POST http://localhost:8081/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=microservices-client&client_secret=microservices-secret-change-me&scope=read"

# Respuesta esperada:
# {
#   "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "refresh_token": "...",
#   "token_type": "Bearer",
#   "expires_in": 3600
# }
```

#### Usar Token OAuth 2.0

```bash
# Guardar access_token y refresh_token
ACCESS_TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
REFRESH_TOKEN="..."

curl -X GET http://localhost:8081/api/auth/users \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json"

# Respuesta esperada: Array JSON de usuarios (igual que con JWT Legacy)
```

#### Refresh Token

```bash
REFRESH_TOKEN="..."

curl -X POST http://localhost:8081/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&client_id=microservices-client&client_secret=microservices-secret-change-me&refresh_token=$REFRESH_TOKEN"

# Respuesta esperada: Nuevos access_token y refresh_token
```

#### Revocar Token

```bash
TOKEN="..."

curl -X POST http://localhost:8081/oauth2/revoke \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=$TOKEN&client_id=microservices-client&client_secret=microservices-secret-change-me"

# Respuesta esperada: HTTP 200 OK (sin body)
```

---

## 📡 Endpoints Disponibles

### Legacy JWT Endpoints (Existentes)

```
POST /api/auth/login              - Login con username/password → JWT HS512
POST /api/auth/register            - Registrar nuevo usuario
GET  /api/auth/users              - Listar usuarios (requiere token)
GET  /api/auth/users/{id}         - Obtener usuario por ID
PUT  /api/auth/users/{id}         - Actualizar usuario
PATCH /api/auth/users/{id}/status - Cambiar estado de usuario
```

### OAuth 2.0 Endpoints (Nuevos)

```
POST /oauth2/token                  - Obtener access token
POST /oauth2/revoke                 - Revocar token
GET  /oauth2/authorize              - Flujo Authorization Code (opcional)
GET  /.well-known/jwks.json        - Obtener claves públicas RSA
GET  /.well-known/oauth-authorization-server - Discovery
```

### OAuth 2.0 Info Endpoints (Nuevos)

```
GET /api/oauth2-info/endpoints            - URLs de endpoints OAuth2
GET /api/oauth2-info/clients              - Info de clientes registrados
GET /api/oauth2-info/example-client-credentials - Ejemplos de uso
GET /api/oauth2-info/migration-info       - Info sobre migración
```

### Otros Endpoints

```
GET /actuator/health                - Health check
GET /actuator/info                  - Información del servicio
```

---

## 🔐 Flujos de Autenticación

### Flujo 1: Legacy JWT (Actual)

```
Cliente
  │
  ├─ POST /api/auth/login
  │  {username: "admin", password: "Admin123"}
  │
  └─ 200 OK
     {
       "token": "eyJhbGciOiJIUzUxMi...",
       "tokenType": "Bearer",
       "userId": "550e8400-..."
     }

    ↓ (Guardar token)

  ├─ GET /api/auth/users
  │  Header: Authorization: Bearer eyJhbGciOiJIUzUxMi...
  │
  └─ 200 OK [array de usuarios]
```

### Flujo 2: OAuth 2.0 Client Credentials

```
Cliente (Microservicio)
  │
  ├─ POST /oauth2/token
  │  {
  │    grant_type: "client_credentials",
  │    client_id: "microservices-client",
  │    client_secret: "microservices-secret-change-me",
  │    scope: "read"
  │  }
  │
  └─ 200 OK
     {
       "access_token": "eyJhbGciOiJSUzI1Ni...",
       "refresh_token": "...",
       "token_type": "Bearer",
       "expires_in": 3600,
       "scope": "read"
     }

    ↓ (Guardar tokens)

  ├─ GET /api/auth/users
  │  Header: Authorization: Bearer eyJhbGciOiJSUzI1Ni...
  │
  └─ 200 OK [array de usuarios]

    ↓ (Después de 1 hora - token expira)

  ├─ POST /oauth2/token
  │  {
  │    grant_type: "refresh_token",
  │    refresh_token: "...",
  │    client_id: "microservices-client",
  │    client_secret: "..."
  │  }
  │
  └─ 200 OK
     {
       "access_token": "eyJhbGciOiJSUzI1Ni...",  // Nuevo
       "refresh_token": "...",                    // Nuevo
       "token_type": "Bearer",
       "expires_in": 3600
     }
```

### Flujo 3: OAuth 2.0 Authorization Code (Frontend SPA)

```
SPA Angular (http://localhost:4200)
  │
  ├─ Redirige a: /oauth2/authorize?
  │  client_id=frontend-app&
  │  response_type=code&
  │  redirect_uri=http://localhost:4200/callback&
  │  scope=openid%20profile
  │
  ├─ Usuario ingresa credenciales en usuario-service
  │
  ├─ Autoriza acceso (si requireAuthorizationConsent=false, se salta)
  │
  ├─ Redirige a: http://localhost:4200/callback?code=AUTH_CODE&state=...
  │
  ├─ SPA: POST /oauth2/token
  │  {
  │    grant_type: "authorization_code",
  │    code: "AUTH_CODE",
  │    client_id: "frontend-app",
  │    client_secret: "frontend-secret-change-me",
  │    redirect_uri: "http://localhost:4200/callback"
  │  }
  │
  └─ 200 OK
     {
       "access_token": "eyJhbGciOiJSUzI1Ni...",
       "refresh_token": "...",
       "token_type": "Bearer",
       "expires_in": 3600,
       "scope": "openid profile"
     }
```

---

## 🆘 Troubleshooting

### Problema 1: "usuario-service not available" en Gateway

**Síntoma:**
```
ERROR: usuario-service not available
404 Service not found
```

**Soluciones:**
1. Verificar que usuario-service está corriendo: `curl http://localhost:8084/actuator/health`
2. Verificar que está registrado en Eureka: `curl http://localhost:8761/eureka/apps`
3. Esperar 30 segundos (tiempo de registro en Eureka)
4. Revisar logs de usuario-service para errores de startup

### Problema 2: "401 Unauthorized" en endpoints protegidos

**Síntoma:**
```
401 Unauthorized
"error": "Token inválido"
```

**Soluciones:**
1. Verificar que el token está en formato correcto: `Authorization: Bearer <token>`
2. Verificar que el token no ha expirado (Legacy: 24h, OAuth2: 1h)
3. Verificar que el token fue obtenido del endpoint correcto:
   - Legacy: `/api/auth/login`
   - OAuth2: `/oauth2/token`
4. Para OAuth2, intentar refrescar token: `POST /oauth2/token` con `grant_type=refresh_token`

### Problema 3: "Connection refused" para PostgreSQL

**Síntoma:**
```
org.postgresql.util.PSQLException: Connection refused
```

**Soluciones:**
1. Verificar que PostgreSQL está corriendo
2. Verificar credenciales en application.yaml:
   - URL: `jdbc:postgresql://localhost:5432/usuarios_db`
   - Usuario: `postgres`
   - Contraseña: `Admin123`
3. Asegurarse que la BD `usuarios_db` existe
4. Asegurarse que la tabla `usuarios` existe

### Problema 4: "Table 'oauth_refresh_tokens' doesn't exist"

**Síntoma:**
```
ERROR: relation "oauth_refresh_tokens" does not exist
```

**Soluciones:**
1. Ejecutar el script SQL:
   ```bash
   psql -U postgres -d usuarios_db -f backend/usuario-service/oauth2_schema.sql
   ```
2. Verificar creación de tabla:
   ```bash
   psql -U postgres -d usuarios_db -c "\dt oauth_refresh_tokens"
   ```

### Problema 5: "Client authentication failed" en OAuth2

**Síntoma:**
```
{"error":"invalid_client","error_description":"Client authentication failed"}
```

**Soluciones:**
1. Verificar `client_id` y `client_secret`:
   - frontend-app: frontend-secret-change-me
   - microservices-client: microservices-secret-change-me
2. Verificar formato de petición (POST, urlencoded, no JSON)
3. Verificar que el cliente está registrado en `AuthorizationServerConfig`

### Problema 6: "Port already in use"

**Síntoma:**
```
ERROR: Port 8084 is already in use
```

**Soluciones:**
1. Encontrar proceso usando el puerto:
   ```bash
   # Linux/Mac
   lsof -i :8084
   # Windows
   netstat -ano | findstr :8084
   ```
2. Matar proceso:
   ```bash
   kill -9 <PID>  # Linux/Mac
   taskkill /PID <PID> /F  # Windows
   ```
3. O cambiar puerto en `application.yaml`

---

## 🔄 Migración Futura

### Fase Actual: Coexistencia (Dual Mode)

```
[Producción]
│
├─ 80% Clientes usan JWT Legacy
├─ 20% Clientes comienzan a usar OAuth 2.0
│
└─ Ambos sistemas funcionan en paralelo
```

### Fase 2: Depreciación de JWT Legacy (3-6 meses)

```
[Producción]
│
├─ 20% Clientes aún usan JWT Legacy
├─ 80% Clientes usan OAuth 2.0
│
└─ JWT Legacy mostrará warnings en logs
```

### Fase 3: Eliminación de JWT Legacy (6-12 meses)

```
[Producción]
│
├─ 0% JWT Legacy
├─ 100% OAuth 2.0
│
└─ JWT Legacy eliminado completamente
```

### Checklist de Migración

- [ ] Documentación actualizada para nuevos clientes
- [ ] Tutorial de migración publicado
- [ ] Ejemplos de código en múltiples lenguajes
- [ ] Soporte técnico disponible para migraciones
- [ ] Fase de depreciación iniciada (3 meses de warnings)
- [ ] Fecha de eliminación comunicada a clientes
- [ ] JWT Legacy finalmente removido

---

## 📚 Recursos Adicionales

### Documentación Externa
- [Spring Authorization Server Docs](https://spring.io/projects/spring-authorization-server)
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [Spring Security Architecture](https://spring.io/guides/topicals/spring-security-architecture)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Archivos en Proyecto
- `backend/usuario-service/oauth2_schema.sql` - Schema de BD para OAuth2
- `backend/usuario-service/usuario-service/pom.xml` - Dependencias Maven
- `backend/usuario-service/usuario-service/src/main/resources/application.yaml` - Configuración
- `backend/api-gateway/api-gateway/src/main/resources/application.yaml` - Rutas Gateway

### Colecciones de Prueba
- `OAuth2-Dual-Tests.postman_collection.json` - Postman collection
- `test-oauth.sh` - Script bash automatizado

---

## ✅ Checklist de Implementación

- [x] Rama feature/oauth2-dual creada
- [x] Dependencias OAuth 2.0 agregadas a pom.xml
- [x] AuthorizationServerConfig implementado
- [x] SecurityConfig actualizado para dual mode
- [x] JwtAuthenticationFilter soporta ambos tipos de tokens
- [x] DualJwtValidator implementado
- [x] OAuthRefreshToken entidad y repositorio
- [x] OAuth2RefreshTokenService implementado
- [x] OAuth2InfoController implementado
- [x] oauth2_schema.sql creado
- [x] application.yaml configurado
- [x] API Gateway actualizado para rutas OAuth
- [x] Test script bash creado
- [x] Postman collection creada
- [x] Documentación completada

---

## 📞 Soporte

Para reportar issues o solicitar ayuda:
1. Revisar la sección Troubleshooting
2. Consultar los logs de los servicios
3. Ejecutar test-oauth.sh para diagnóstico
4. Contactar al equipo de infraestructura

---

**Última actualización**: Mayo 2026  
**Estado**: ✅ Implementación Completa  
**Rama**: feature/oauth2-dual
