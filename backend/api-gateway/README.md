# 🌐 API Gateway - Puerta de Entrada del Sistema

## 📋 Descripción General

**API Gateway** es el punto de entrada centralizado para todos los clientes externos que desean acceder a los microservicios.

**Puerto:** `8081`  
**Responsabilidad:** Enrutamiento, autenticación, rate limiting  
**Patrón:** API Gateway Pattern

---

## 🎯 Funciones Principales

### 1. **Enrutamiento de Solicitudes**
Distribuye las solicitudes hacia el microservicio correcto basado en la ruta.

```
Cliente → API Gateway (8081) → Determina servicio → Microservicio
                                      ↓
                    /api/auth/*      → usuario-service (8084)
                    /api/suppliers/* → proveedor-service (8086)
                    /api/contracts/* → contrato-service (8087)
                    /api/audit/*     → audit-service (8085)
```

### 2. **Autenticación y Autorización**
Valida tokens JWT en cada solicitud antes de enrutar.

```
Cliente
    ↓
POST /api/auth/login
    ↓ (Sin token - Público)
Usuario-Service
    ↓ (Retorna JWT)
Cliente ahora tiene token
    ↓
GET /api/auth/users (Con token)
    ↓ (API Gateway valida JWT)
✅ Token válido → Enrutar a usuario-service
❌ Token inválido → Retorna 401 Unauthorized
```

### 3. **Descubrimiento de Servicios**
Se integra con Eureka para resolver dinámicamente las direcciones de los microservicios.

```
API Gateway
    ↓
¿Dónde está usuario-service?
    ↓
Pregunta a Eureka
    ↓
Eureka responde: localhost:8084
    ↓
Enrutar solicitud a localhost:8084
```

---

## 📡 Configuración de Rutas

### Configuración en application.yaml
```yaml
spring:
  cloud:
    gateway:
      routes:
        # Ruta 1: Usuario Service
        - id: usuario-service
          uri: lb://usuario-service
          predicates:
            - Path=/api/auth/**
          
        # Ruta 2: Proveedor Service
        - id: proveedor-service
          uri: lb://proveedor-service
          predicates:
            - Path=/api/suppliers/**
          
        # Ruta 3: Contrato Service
        - id: contrato-service
          uri: lb://contrato-service
          predicates:
            - Path=/api/contracts/**
          
        # Ruta 4: Audit Service
        - id: audit-service
          uri: lb://audit-service
          predicates:
            - Path=/api/audit/**
```

### Explicación de Rutas
```
lb://usuario-service
 ↑
 Load Balanced (Eureka lo resuelve)
```

---

## 🔐 Filtros de Seguridad

### JWT Authentication Filter
Todas las solicitudes pasan por este filtro (excepto `/api/auth/login`).

```
Solicitud → JwtAuthenticationFilter
                ↓
        ¿Tiene Authorization header?
                ↓
        Extrae token de "Bearer {token}"
                ↓
        ¿Es token válido?
                ↓
        Sí ✅ → Continúa
        No ❌ → 401 Unauthorized
```

---

## 🚀 Despliegue

### Build
```bash
cd api-gateway
mvn clean package -DskipTests
```

### Ejecutar
```bash
# Opción 1: Maven
mvn spring-boot:run

# Opción 2: Java JAR
java -jar target/api-gateway-1.0.0.jar

# Opción 3: Docker
docker build -t api-gateway:1.0 .
docker run -p 8081:8081 api-gateway:1.0
```

---

## 📊 Flujo de Solicitudes

### Login (Sin autenticación)
```
Cliente
  │
  ├─→ POST http://localhost:8081/api/auth/login
  │   {username: "admin", password: "pass"}
  │
  ├─→ API Gateway (puerto 8081)
  │   [Sin JWT requerido]
  │
  ├─→ Usuario-Service (puerto 8084)
  │   [Autentica credenciales]
  │
  └─→ Retorna JWT Token
      {token: "eyJ..."}
```

### Solicitud Protegida (Con autenticación)
```
Cliente
  │
  ├─→ GET http://localhost:8081/api/auth/users
  │   Header: Authorization: Bearer eyJ...
  │
  ├─→ API Gateway (puerto 8081)
  │   [JwtAuthenticationFilter]
  │   └─→ Valida JWT
  │       ✅ Token válido
  │
  ├─→ Usuario-Service (puerto 8084)
  │   [Procesa solicitud]
  │
  └─→ Retorna datos
      {users: [...]}
```

---

## 📡 Endpoints Disponibles

### Todos los endpoints acceden a través del Gateway

```
Login:
POST http://localhost:8081/api/auth/login

Usuarios:
POST   /api/auth/register
GET    /api/auth/me
GET    /api/auth/users
GET    /api/auth/users/{id}
PUT    /api/auth/users/{id}
PATCH  /api/auth/users/{id}/status
DELETE /api/auth/users/{id}

Proveedores:
GET    /api/suppliers
POST   /api/suppliers
GET    /api/suppliers/{id}
PUT    /api/suppliers/{id}
DELETE /api/suppliers/{id}

Contratos:
GET    /api/contracts
POST   /api/contracts
GET    /api/contracts/{id}
PUT    /api/contracts/{id}
PATCH  /api/contracts/{id}/status

Auditoría:
GET    /api/audit/events
GET    /api/audit/events/{id}
```

---

## ⚙️ Configuración

### Variables de Entorno
```env
# Server
SERVER_PORT=8081

# Eureka
EUREKA_URL=http://localhost:8761/eureka/

# JWT
JWT_SECRET_KEY=your-super-secret-key-min-256-bits-for-hs512
```

### application.yaml Completa
```yaml
server:
  port: 8081

spring:
  application:
    name: api-gateway
  
  cloud:
    gateway:
      # Configurar para resolver servicios desde Eureka
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      
      # Rutas específicas
      routes:
        - id: usuario-service
          uri: lb://usuario-service
          predicates:
            - Path=/api/auth/**
        
        - id: proveedor-service
          uri: lb://proveedor-service
          predicates:
            - Path=/api/suppliers/**
        
        - id: contrato-service
          uri: lb://contrato-service
          predicates:
            - Path=/api/contracts/**
        
        - id: audit-service
          uri: lb://audit-service
          predicates:
            - Path=/api/audit/**

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
    register-with-eureka: true
    fetch-registry: true
  instance:
    hostname: localhost
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

---

## 🔍 Monitoreo

### Health Check
```bash
curl http://localhost:8081/actuator/health
```

**Respuesta:**
```json
{
  "status": "UP",
  "components": {
    "discoveryClient": {
      "status": "UP",
      "details": {
        "services": ["api-gateway", "usuario-service", ...]
      }
    }
  }
}
```

---

## 🛠️ Troubleshooting

### Problema 1: "Service not found"
```
Error: usuario-service not available
Causa: El servicio no está registrado en Eureka
Solución:
1. Verificar que usuario-service está corriendo
2. Verificar que Eureka está corriendo
3. Esperar 30 segundos (tiempo de registro)
```

### Problema 2: "401 Unauthorized"
```
Error: Token inválido o expirado
Solución:
1. Hacer login nuevamente para obtener nuevo token
2. POST /api/auth/login
3. Usar nuevo token en Authorization header
```

### Problema 3: Timeout en solicitudes
```
Causa: Servicio lento o no responde
Solución:
1. Revisar logs del microservicio
2. Aumentar timeout en Gateway
3. Verificar conectividad de red
```

---

## 📊 Logs Esperados

### Al iniciar Gateway
```log
INFO : Netty started with worker group 'reactor-http-worker-epoll'
INFO : Route [usuario-service] routes to lb://usuario-service
INFO : Route [proveedor-service] routes to lb://proveedor-service
INFO : Route [contrato-service] routes to lb://contrato-service
INFO : Route [audit-service] routes to lb://audit-service
INFO : Initializing Eureka in region us-east-1
INFO : DiscoveryClient initialized at timestamp 1234567890
```

### Cuando recibe solicitud
```log
DEBUG: Gateway routing request [POST /api/auth/login] to [usuario-service]
DEBUG: Forwarding request to [http://localhost:8084/api/auth/login]
INFO : Received 200 response from usuario-service
```

---

## 🔐 Consideraciones de Seguridad

### En Producción
- ✅ Implementar rate limiting
- ✅ CORS configurado restrictivamente
- ✅ HTTPS (SSL/TLS)
- ✅ WAF (Web Application Firewall)
- ❌ NO exponer errores internos
- ❌ NO loguear información sensitiva

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────┐
│                   API Gateway (8081)                │
├─────────────────────────────────────────────────────┤
│  Contenedor de Solicitud                            │
│  ├─ JwtAuthenticationFilter (Seguridad)            │
│  ├─ DispatcherServlet (Enrutamiento)               │
│  └─ Gateway Filters (Transformaciones)             │
├─────────────────────────────────────────────────────┤
│  Service Discovery (Eureka Client)                  │
├─────────────────────────────────────────────────────┤
│            Conexiones a Microservicios              │
│  ├─ Usuario-Service    (8084)  ----┐               │
│  ├─ Proveedor-Service  (8086)  ----├─ Eureka (8761)
│  ├─ Contrato-Service   (8087)  ----│               │
│  └─ Audit-Service      (8085)  ────┘               │
└─────────────────────────────────────────────────────┘
```

---

## 📝 Ejemplo de Solicitud Completa

### 1. Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "admin",
    "email": "admin@example.com",
    "role": "ADMINISTRADOR",
    "active": true
  }
}
```

### 2. Usar Token en Solicitud Protegida
```bash
curl -X GET http://localhost:8081/api/auth/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."
```

**Respuesta:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "admin",
    "email": "admin@example.com",
    "role": "ADMINISTRADOR",
    "active": true
  }
]
```

---

## 👨‍💻 Desarrollador Responsable

**Dev1 - Infraestructura**
- Lina Xiomara Ladino Fernández
- Revisor: Sebastian Perez (Arquitecto Senior)

---

**Última actualización:** 26 de Abril de 2026  
**Versión:** 1.0  
**Estado:** ✅ Production Ready
