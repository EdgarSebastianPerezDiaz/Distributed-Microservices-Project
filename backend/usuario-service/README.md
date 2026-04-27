# 👥 Usuario Service - Microservicio de Autenticación y Gestión de Usuarios

## 📋 Descripción General

**Usuario Service** es el microservicio responsable de:
- ✅ Autenticación de usuarios (Login)
- ✅ Gestión de usuarios (CRUD)
- ✅ Generación y validación de tokens JWT (HS512)
- ✅ Control de roles y permisos (RBAC)
- ✅ Hashing de contraseñas con SHA-512
- ✅ Integración con servicio de auditoría
- ✅ Descubrimiento automático en Eureka

**Puerto:** `8084`  
**Registrado en Eureka:** `usuario-service`  
**Base de Datos:** PostgreSQL (db_usuarios)

---

## 🏗️ Arquitectura

```
usuario-service/
├── src/main/java/com/distribuidos/usuario_service/
│   ├── UsuarioServiceApplication.java      (Clase principal)
│   ├── controller/
│   │   └── AuthController.java             (Endpoints REST)
│   ├── service/
│   │   └── UserService.java                (Lógica de negocio)
│   ├── model/
│   │   ├── User.java                       (Entidad Usuario)
│   │   └── Role.java                       (Entidad Rol)
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── RoleRepository.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── UserRequest.java
│   │   ├── UserResponse.java
│   │   ├── UserUpdateRequest.java
│   │   └── AuditEventDTO.java
│   ├── security/
│   │   ├── JwtService.java                 (Generación/validación JWT)
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── SecurityUtils.java              (Hashing SHA-512)
│   │   └── SecurityConfig.java
│   ├── exception/
│   │   ├── UserAlreadyExistsException.java
│   │   ├── UserNotFoundException.java
│   │   ├── InvalidCredentialsException.java
│   │   ├── InvalidRoleException.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── ErrorResponse.java
│   ├── client/
│   │   └── AuditClient.java                (Cliente Feign para Audit Service)
│   └── config/
│       └── DataInitializer.java
└── src/main/resources/
    └── application.yaml

```

---

## 🔐 Seguridad

### Autenticación
- **Algoritmo JWT:** HS512 (HMAC SHA-512)
- **Clave secreta:** Compartida entre servicios (variable de entorno)
- **Claims incluidos:**
  - `userId` (UUID del usuario)
  - `username` (Nombre de usuario)
  - `role` (Rol del usuario)

### Hash de Contraseñas
- **Algoritmo:** SHA-512
- **Implementación:** `com.distribuidos.usuario_service.security.SecurityUtils`
- **Validación:** Las contraseñas nunca se almacenan en texto plano

### Roles (RBAC)
```
ADMINISTRADOR  → Acceso total (crear/editar/eliminar usuarios)
FUNCIONARIO    → Acceso limitado (leer información)
AUDITOR        → Solo lectura (auditoría y reportes)
```

---

## 📡 API Endpoints

### 1️⃣ **Login** (Público)
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**Respuesta (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "admin",
    "email": "admin@example.com",
    "fullName": "Administrator",
    "role": "ADMINISTRADOR",
    "active": true,
    "createdAt": "2026-03-24T10:00:00",
    "lastLogin": "2026-04-26T15:30:00"
  }
}
```

---

### 2️⃣ **Crear Usuario** (Solo ADMINISTRADOR)
```http
POST /api/auth/register
Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "newuser",
  "password": "SecurePassword123!",
  "email": "newuser@example.com",
  "fullName": "New User",
  "role": "FUNCIONARIO"
}
```

**Respuesta (201 Created):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440000",
  "username": "newuser",
  "email": "newuser@example.com",
  "fullName": "New User",
  "role": "FUNCIONARIO",
  "active": true,
  "createdAt": "2026-04-26T15:35:00",
  "lastLogin": null
}
```

---

### 3️⃣ **Obtener Perfil Actual** (Autenticado)
```http
GET /api/auth/me
Authorization: Bearer {token}
```

**Respuesta (200 OK):** Retorna los datos del usuario autenticado

---

### 4️⃣ **Listar Todos los Usuarios** (Solo ADMINISTRADOR)
```http
GET /api/auth/users
Authorization: Bearer {token}
```

**Respuesta (200 OK):** Array de usuarios

---

### 5️⃣ **Obtener Usuario por ID** (Solo ADMINISTRADOR)
```http
GET /api/auth/users/{id}
Authorization: Bearer {token}
```

---

### 6️⃣ **Actualizar Usuario** (Solo ADMINISTRADOR)
```http
PUT /api/auth/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "email": "updated@example.com",
  "fullName": "Updated Name",
  "role": "AUDITOR"
}
```

---

### 7️⃣ **Cambiar Estado (Activar/Desactivar)** (Solo ADMINISTRADOR)
```http
PATCH /api/auth/users/{id}/status
Authorization: Bearer {token}
```

**Nota:** No puede desactivarse a sí mismo

---

### 8️⃣ **Eliminar Usuario** (Solo ADMINISTRADOR)
```http
DELETE /api/auth/users/{id}
Authorization: Bearer {token}
```

**Respuesta (204 No Content)**

---

## 🛠️ Configuración

### Variables de Entorno Requeridas
```env
# Base de Datos
DB_USER=postgres
DB_PASSWORD=Admin123
DB_NAME=db_usuarios
DB_HOST=localhost
DB_PORT=5432

# JWT
JWT_SECRET_KEY=your-super-secret-key-min-256-bits-for-hs512

# Eureka
EUREKA_URL=http://localhost:8761/eureka/

# Auditoría
AUDIT_SERVICE_URL=http://localhost:8085
```

### application.yaml
```yaml
server:
  port: 8084

spring:
  application:
    name: usuario-service
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:db_usuarios}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:Admin123}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
    fetch-registry: true
    register-with-eureka: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

## 🧪 Testing

### Tests Unitarios Incluidos
- ✅ **AuthControllerTest** - 9 test cases
- ✅ **UserServiceTest** - 15 test cases
- ✅ **Total cobertura:** 24+ test cases

### Ejecutar Tests
```bash
# Dentro del directorio usuario-service/
mvn test

# O ejecutar tests específicos
mvn test -Dtest=UserServiceTest
mvn test -Dtest=AuthControllerTest
```

### Cobertura de Tests
- Login exitoso/fallido
- Crear usuario (validaciones)
- Obtener usuario (éxito/no encontrado)
- Actualizar usuario
- Cambiar estado
- Eliminar usuario
- Manejo de excepciones

---

## ⚠️ Manejo de Errores

### Excepciones Personalizadas
```
UserNotFoundException        → 404 Not Found
UserAlreadyExistsException   → 409 Conflict
InvalidCredentialsException  → 401 Unauthorized
InvalidRoleException         → 400 Bad Request
```

### Formato de Respuesta de Error
```json
{
  "timestamp": "2026-04-26T15:45:30",
  "status": 404,
  "error": "USER_NOT_FOUND",
  "message": "Usuario con ID 550e8400-e29b-41d4-a716-446655440000 no encontrado",
  "path": "/api/auth/users/550e8400-e29b-41d4-a716-446655440000",
  "validationErrors": null
}
```

---

## 🔗 Integración con Otros Servicios

### Comunicación con Audit Service
- **Cliente:** OpenFeign (`AuditClient`)
- **Eventos registrados:**
  - `CREAR_USUARIO` - Cuando se crea un nuevo usuario
  - `MODIFICAR_USUARIO` - Cuando se actualiza un usuario
  - `ELIMINAR_USUARIO` - Cuando se elimina un usuario
  - Cambios de estado (ACTIVO → INACTIVO)

### Comunicación con Eureka
- Se registra automáticamente en startup
- Health checks cada 30 segundos
- Se desregistra al shutdown

---

## 📊 Base de Datos

### Tabla: Users
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    CONSTRAINT uk_username UNIQUE(username),
    CONSTRAINT uk_email UNIQUE(email)
);
```

### Tabla: Roles
```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Datos iniciales
INSERT INTO roles (name) VALUES ('ADMINISTRADOR');
INSERT INTO roles (name) VALUES ('FUNCIONARIO');
INSERT INTO roles (name) VALUES ('AUDITOR');
```

---

## 🚀 Despliegue

### Build
```bash
mvn clean package -DskipTests
```

### Ejecutar
```bash
# Con Maven
mvn spring-boot:run

# Con Java
java -jar usuario-service.jar

# Con Docker
docker build -t usuario-service:1.0 .
docker run -p 8084:8084 usuario-service:1.0
```

---

## 📝 Logs Importantes

```log
INFO: ========================================
INFO: ✅ USUARIO SERVICE INICIADO
INFO: 📍 URL: http://localhost:8084
INFO: 📋 Endpoints disponibles:
INFO:    POST /api/auth/login    (Login - Público)
INFO:    POST /api/auth/register (Crear usuario - Admin)
INFO:    GET  /api/auth/me       (Perfil actual)
INFO:    GET  /api/auth/users    (Listar usuarios)
INFO: ========================================
```

---

## 🔍 Monitoreo y Health Check

### Health Endpoint
```http
GET http://localhost:8084/actuator/health
```

**Respuesta:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "version": "13.x"
      }
    },
    "discoveryClient": {
      "status": "UP",
      "details": {
        "services": ["usuario-service", "eureka-service", ...]
      }
    }
  }
}
```

---

## ❤️ Desarrollador Responsable

**Dev1 - Infraestructura y Seguridad**
- Lina Xiomara Ladino Fernández
- Revisor: Sebastian Perez (Arquitecto Senior)

---

## 📅 Historial de Cambios

### v1.2 (26 de Abril de 2026)
- ⭐ Añadido Global Exception Handler
- ⭐ Nuevas excepciones personalizadas
- ⭐ Tests unitarios comprehensivos (24+ test cases)
- ⭐ DELETE endpoint para usuarios
- ⭐ Logging mejorado
- ⭐ Documentación completa

### v1.1 (18 de Abril de 2026)
- Edición de perfiles de usuario
- Integración de auditoría

### v1.0 (8 de Abril de 2026)
- Release inicial con login y CRUD

---

## 📚 Referencias

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [JWT (json-web-token)](https://jwt.io)
- [OpenFeign](https://spring.io/projects/spring-cloud-openfeign)

---

**Última actualización:** 26 de Abril de 2026  
**Versión:** 1.2  
**Estado:** ✅ Production Ready
