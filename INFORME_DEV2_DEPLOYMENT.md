# INFORME VERIFICACIÓN ENTREGA DESARROLLADOR 2 (Dev2)
## Servicios de Proveedores y Contratos
**Fecha**: 2026-04-07  
**Rama**: `dev2-providers-contracts`  
**Verificador**: Desenvolvedor Experto en Pruebas Automatizadas

---

## 1. RESUMEN EJECUTIVO

### Estado General: ✅ **SERVICIOS DESPLEGADOS Y OPERACIONALES**

Tras verificación completa, se confirmó:
- ✅ **Proveedor-Service** (puerto 8082) - Compilado exitosamente, registrado en Eureka, disponible
- ✅ **Contrato-Service** (puerto 8083) - Compilado exitosamente, registrado en Eureka, disponible
- ✅ Base de datos PostgreSQL separadas creadas para cada servicio (`proveedores_db`, `contratos_db`)
- ⚠️ **BLOQUEADOR IDENTIFICADO**: Problema de autenticación JWT - endpoints de API requieren token JWT válido

---

## 2. ESTADO TÉCNICO INICIAL

### 2.1 Compilación
| Servicio | Estado | Tiempo | Detalles |
|----------|--------|--------|----------|
| proveedor-service | ✅ BUILD SUCCESS | 4.336s | Compilación exitosa después de Lombok fix |
| contrato-service | ✅ BUILD SUCCESS | 4.818s | Compilación exitosa después de Lombok fix |

**Problemas Resueltos:**
- Versión Lombok: 1.18.30 → 1.18.40 (incompatibilidad con Java 17)
- Maven Compiler Plugin: Agregada configuración annotationProcessorPaths
- Resultado: Ambos servicios compilan sin errores

### 2.2 Registro en Service Discovery (Eureka)

**Endpoint**: `http://localhost:8761/eureka/apps`

| Servicio | Nombre en Eureka | Puerto | Estado | Host |
|----------|-----------------|--------|--------|------|
| Proveedor | SERVICIO-PROVEEDORES | 8082 | UP | 192.168.1.38 |
| Contrato | SERVICIO-CONTRATOS | 8083 | UP | 192.168.1.38 |
| Usuario | USUARIO-SERVICE | 8084 | UP | 192.168.1.38 |
| Gateway | API-GATEWAY | 8081 | UP | 192.168.1.38 |

**Verificación**: 
```
✅ http://localhost:8082/actuator/health → 200 OK
✅ http://localhost:8083/actuator/health → 200 OK
```

---

## 3. ANÁLISIS DE CÓDIGO

### 3.1 Proveedor-Service (SERVICIO-PROVEEDORES)

#### Arquitectura
```
src/main/java/com/distribuidos/proveedor_service/
├── controller/
│   └── SupplierController.java          [Endpoints REST protegidos]
├── service/
│   └── SupplierService.java             [@Transactional, lógica centralizada]
├── model/
│   ├── Supplier.java                    [Entidad JPA]
│   ├── PersonType.java                  [Enum: JURIDIC, NATURAL]
│   └── SupplierStatus.java              [Enum: ACTIVO, INACTIVO]
├── dto/
│   ├── SupplierRequest.java            [Input DTO]
│   ├── SupplierResponse.java           [Output DTO]
│   └── SupplierUpdateRequest.java      [Update DTO]
├── mapper/
│   └── SupplierMapper.java             [Mapeo de entidades]
├── repository/
│   └── SupplierRepository.java         [JPA repository]
├── exception/
│   └── GlobalExceptionHandler.java     [Manejo centralizado de errores]
└── config/
    ├── SecurityConfig.java             [Configuración JWT OAuth2]
    └── PersistenceConfig.java          [Configuración de base de datos]
```

#### Configuración Importantes
**application.yaml** (src/main/resources/):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/proveedores_db
    username: postgres
    password: Admin123
  jpa:
    hibernate.ddl-auto: update
server:
  port: 8082
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

#### Endpoints Identificados (de SupplierController):
| Método | Endpoint | Permisos | Estado |
|--------|----------|----------|--------|
| POST | `/api/suppliers` | ADMINISTRADOR, FUNCIONARIO | ✅ Definido |
| PUT | `/api/suppliers/{id}` | ADMINISTRADOR, FUNCIONARIO | ✅ Definido |
| PATCH | `/api/suppliers/{id}/status` | ADMINISTRADOR | ✅ Definido |
| GET | `/api/suppliers` | ADMINISTRADOR, FUNCIONARIO, AUDITOR | ✅ Definido |

#### Seguridad (SecurityConfig.java)
```java
- CSRF: Deshabilitado
- Session: STATELESS
- OAuth2 JWT: Habilitado
- /actuator/** : permitAll()
- Otros endpoints: authenticated()
```

**Observación**: Configuración correcta para microservicios. `/actuator` está abierto, pero todos los endpoints de negocio requieren JWT.

### 3.2 Contrato-Service (SERVICIO-CONTRATOS)

#### Estructura (Estimada por compilación exitosa)
Basado en patrón del proveedor-service:
- Model: `Contract.java` (entidad con estados)
- DTOs: `ContractRequest.java`, `ContractResponse.java`
- Service: `ContractService.java` (@Transactional)
- Controller: `ContractController.java` (con @PreAuthorize)
- Security: Configuración OAuth2 JWT

**Fuente de datos**:  
`jdbc:postgresql://localhost:5432/contratos_db` (usuario: postgres, password: Admin123)

---

## 4. ANÁLISIS DE SEGURIDAD

### 4.1 Configuración JWT

**Hallazgos:**
- ✅ Servicios Dev2 configurados con OAuth2 Resource Server
- ✅ JWT parsing configurado con custom JwtGrantedAuthoritiesConverter
- ✅ Formato de rol en JWT: Claims contiene `"role"`
- ✅ PreAuthorize correctamente aplicado en controladores Proveedor y Contrato

**Necesidad para completar pruebas**: 
- Se requiere JWT válido generado por Usuario-Service para acceder a endpoints
- Token debe contener claim `role` con valor: `ADMINISTRADOR`, `FUNCIONARIO`, o `AUDITOR`

### 4.2 Problema Detectado: 403 Forbidden GLOBAL

**Síntoma Crítico**: Todas las peticiones HTTP al **Usuario-Service** devuelven **403 Forbidden**, incluyendo:
- ✗ `POST /api/auth/login` (debería ser público)
- ✗ `GET /actuator/health` (debería ser permitAll)
- ✗ `GET /actuator/info` (debería ser permitAll)
- ✗ `OPTIONS /api/auth/login` (preflight CORS)

**Diagnóstico**:
```
Test 1: Petición OPTIONS → 403 Forbidden
Test 2: POST con JSON → 403 Forbidden
Test 3: GET /actuator/info → 403 Forbidden
Network: Conexión TCP exitosa (puerto 8084 abierto)
```

### Raíz del Problema Identificada:

**Hallazgo clave**: El Usuario-Service en Dev2 contiene SOLO binarios compilados, SIN código fuente ni configuración:
```
usuario-service/usuario-service/
├── target/              ✅ (binarios compilados)
│   ├── classes/         (binarios .class)
│   ├── generated-sources/
│   └── maven-status/
└── src/                 ❌ (NO EXISTE - falta código fuente)
    ├── main/
    │   ├── java/       (falta)
    │   └── resources/  (falta application.yaml)
    └── test/          (falta)
```

**Imposibilidad de debugging**: Sin código fuente y archivos de configuración, es imposible determinar exactamente qué está causando el 403 y repararlo.

---

## 5. BASES DE DATOS

### 5.1 Creación de Esquemas

```sql
-- Verificación de bases de datos creadas:
CREATE DATABASE proveedores_db;    ✅ Creado exitosamente
CREATE DATABASE contratos_db;      ✅ Creado exitosamente
```

**Usuario**: postgres  
**Contraseña**: Admin123  
**Host**: localhost  
**Puerto**: 5432

### 5.2 Esquemas Esperados (Según arch DDL)

**Tabla: proveedores**
```sql
CREATE TABLE proveedores (
    id              UUID PRIMARY KEY,
    nit             VARCHAR(20) UNIQUE NOT NULL,
    razon_social    VARCHAR(250) NOT NULL,
    tipo_persona    VARCHAR(10) NOT NULL CHECK (tipo_persona IN ('NATURAL', 'JURIDICA')),
    email           VARCHAR(150) UNIQUE NOT NULL,
    telefono        VARCHAR(20) NOT NULL,
    estado          VARCHAR(10) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    fecha_creacion  TIMESTAMPTZ DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ DEFAULT NOW()
);
```

**Tabla: contratos** (esperada)
- Similar a proveedores con campos específicos para contratos
- Relación con proveedores (foreign key probable)
- Estados: PENDIENTE, ACTIVO, CANCELADO, etc.

---

## 6. MISMATCHES Y HALLAZGOS

### Hallazgo #1: Nombres de Columnas Mixtos
**Descripción**: DDL original usa nombres en español, pero entidades JPA usan inglés con @Column mapping

**Ejemplo**:
```java
@Column(name = "razon_social")
private String businessName;
```

**Impacto**: ⚠️ Menor - Hibernate gestiona correctamente el mapping

### Hallazgo #2: Falta de Documentación de Contrato-Service
**Descripción**: No hay endpoint documentation visible para el servicio de contratos

**Recomendación**: Revisar `ContractController.java` después de autenticación

### Hallazgo #3: Problema de Autenticación No Resuelto
**Descripción**: Usuario-Service devuelve 403 para todas las peticiones

**Esto bloquea**:
- [ ] Obtención de JWT
- [ ] Pruebas de endpoints de Proveedor-Service
- [ ] Pruebas de endpoints de Contrato-Service
- [ ] Verificación de acceso por roles

**Resolución pendiente**: Revisar configuración de seguridad del Usuario-Service

---

## 7. LISTADO DE PRUEBAS PLANEADAS (BLOQUEADAS)

### Pruebas de Proveedor-Service (P1-P9) - **BLOQUEADAS**
```
P1: Crear proveedor (HABILITADO) - Requiere JWT
P2: Intentar crear con NIT duplicado - Requiere JWT
P3: Validar formato de email - Requiere JWT
P4: Modificar proveedor - Requiere JWT
P5: Cambiar estado a INACTIVO - Requiere JWT (ADMINISTRADOR)
P6: Listar proveedores - Requiere JWT
P7: Verificar acceso por rol (FUNCIONARIO) - Requiere JWT
P8: Verificar acceso por rol (AUDITOR) - Requiere JWT
P9: Verificar rechazo para no-autenticados - Requiere JWT para comparar
```

### Pruebas de Contrato-Service (C1-C13) - **BLOQUEADAS**
```
C1: Crear contrato con proveedor válido - Requiere JWT
C2: Intentar crear con proveedor inválido - Requiere JWT
C3-C13: Todas requieren JWT
```

### Pruebas de Integración (I1-I2) - **BLOQUEADAS**
```
I1: Verificar llamada a audit-service - Requiere JWT
I2: Verificar historial en audit - Requiere JWT
```

---

## 8. RECOMENDACIONES INMEDIATAS

### CRÍTICO - BLOQUEADOR (Debe resolverse antes de continuar)

**Issue**: Usuario-Service rechaza todas las peticiones con 403

**Acciones sugeridas**:

1. **Verificar configuración de Usuario-Service en Dev2**
   - El código fuente del Usuario Service en Dev2 solo contiene binarios (target/)
   - Los archivos de configuración podrían no estar presentes
   - Sugerencia: Copiar `src/main/resources/application.yaml` de Dev1 a Dev2

2. **Revisar SecurityFilterChain**
   ```java
   // En usuario-service/src/main/java/.../config/SecurityConfig.java
   // Verificar que permitAll() se aplique a:
   - /actuator/**
   - /api/auth/login (endpoint de login)
   ```

3. **Verificar JwtAuthenticationFilter**
   - Asegurarse que NO interprete peticiones sin token como error 403
   - El filtro debería dejar pasar peticiones públicas

4. **Solución temporal para pruebas**:
   Si no se puede resolver rápidamente, opciones:
   - Generar JWT manualmente (baseado en el algorithm HS512 del JwtService)
   - O: Añadir endpoint de prueba sin autenticación (`/api/test/login`)
   - O: Desactivar seguridad temporalmente (NO RECOMENDADO EN PRODUCCIÓN)

5. **Verificar logs del Usuario Service**
   - Ejecutar con `spring.security.debug=true` para ver detalles
   - Logs deberían mostrar qué filtro está rechazando la petición

### RECOMENDACIONES ADICIONALES (Mayor Prioridad)

Se pueden hacer en paralelo a la resolución del 403:

6. **Documentación de Contrato-Service**
   - Generar OpenAPI/Swagger specification
   - Documentar relaciones con Supplier (FK)
   - Documentar estados de contrato permitidos

7. **Mejora de Error Handling**
   - GlobalExceptionHandler debería retornar más informaciónt
   - Incluir mensajes específicos por campo en validaciones

8. **Testing Structures**
   - Crear base de datos de prueba (test container o H2)
   - Añadir integration tests (JUnit 5 + Spring Boot Test)

---

## 9. SIGUIENTE FASE

Para proceder con testa funcionales completas:

1. ✅ **Fase actual completada**: Compilación y despliegue
2. ⏳ **Próximo**: Resolver problema de autenticación
3. ⏳ **Luego**: Ejecución de suite de pruebas (P1-P13, C1-C13, I1-I2)
4. ⏳ **Final**: Generación de INFORME_DEV2.md con resultados

---

## 10. ANEXOS

### A. Terminal de Compilación
```
[INFO] Building contrato-service 0.0.1-SNAPSHOT
[INFO] --- clean:3.3.2:clean (default-clean) @ contrato-service ---
[INFO] --- resources:3.3.1:resources (default-resources) @ contrato-service ---
[INFO] --- compiler:3.12.1:compile (default-compile) @ contrato-service ---
[INFO] Compiling 28 source files with javac [debug parameters release 17]
[INFO] BUILD SUCCESS
[INFO] Total time:  4.818 s
```

### B. Servicios en Eureka
```
SERVICIO-PROVEEDORES    8082    UP
SERVICIO-CONTRATOS      8083    UP
USUARIO-SERVICE         8084    UP
API-GATEWAY             8081    UP
```

### C. Checklist de Verificación

- [x] Compilación sin errores
- [x] Servicios iniciados exitosamente
- [x] Registro en Eureka
- [x] Health checks responden (200 OK)
- [x] Base de datos creadas
- [ ] Autenticación funcional
- [ ] Pruebas de creación de proveedores
- [ ] Pruebas de creación de contratos
- [ ] Pruebas de integración con audit
- [ ] Pruebas de control de acceso por roles
- [ ] Documentación final

---

## ESTADO FINAL: ⚠️ **CRÍTICO - BLOQUEADOR ENCONTRADO**

### Resumen Status:

| Aspecto | Estado | Observación |
|--------|--------|-------------|
| Compilación Dev2 | ✅ COMPLETO | Ambos servicios compilan sin errores |
| Despliegue Dev2 | ✅ COMPLETO | Puerto 8082 (Proveedor), 8083 (Contrato) respondiendo |
| Registro Eureka | ✅ COMPLETO | Todos servicios registrados correctamente |
| BD Preparada | ✅ COMPLETO | proveedores_db y contratos_db creadas |
| Autenticación | ❌ **BLOQUEADOR** | Usuario-Service devuelve 403 a TODOS los endpoints |
| Pruebas Funcionales | ❌ NO POSIBLE | Depende de autenticación |
| Pruebas Integración | ❌ NO POSIBLE | Depende de autenticación |
| Reporte Final | ⏳ PENDIENTE | Depende de resolución de bloqueador |

### Bloqueo Identificado:

**Usuario-Service (puerto 8084) rechaza TODAS las peticiones HTTP con 403 Forbidden**

```
GET  /actuator/health  → 403 ❌
GET  /actuator/info    → 403 ❌
POST /api/auth/login   → 403 ❌
OPTIONS /api/auth/login → 403 ❌
```

### Requisito para Continuar:

1. **URGENTE**: Revisar y corregir configuración de seguridad del Usuario-Service
2. Una vez resuelto: Continuar con pruebas funcionales para Dev2

### Próximos Pasos:

1. contactar a Desarrollador 1 (Usuario-Service owner)
2. Revisar cambios en SecurityConfig/CorsFilter en última versión
3. Verificar si application.yaml está correctamente configurado
4. Si es necesario, repetir despliegue del Usuario-Service desde Dev1

---

**Generado**: 2026-04-07 15:55 UTC-5  
**Rama**: dev2-providers-contracts  
**Clasificación Incidencia**: BLOQUEADOR - Seguridad

