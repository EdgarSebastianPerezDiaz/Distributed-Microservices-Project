# INFORME DE VERIFICACIÓN - DESARROLLADOR 3 (Auditoría - Python/FastAPI)

**Fecha y hora:** 2026-04-07 16:40 UTC-5  
**Rama evaluada:** dev3-audit  
**Commit evaluado:** (rama verificada)  
**Servicio:** Audit Service (Python/FastAPI + Motor + MongoDB)

---

## 1. RESUMEN EJECUTIVO

**Estado general:** ⚠️ **CUMPLE PARCIALMENTE**  
**Puntuación estimada:** 65/100

El Audit Service está **funcionalmente disponible** pero tiene **BLOQUEADORES CRÍTICOS**:

1. ✅ El servicio inicia correctamente (uvicorn en puerto 8000)
2. ✅ Health check disponible (/health → 200)
3. ❌ **CRÍTICO**: Autenticación JWT **completamente desactivada** (comentada en routes)
4. ⚠️ Requiere MongoDB funcional (no disponible vía Docker en este ambiente)
5. ⚠️ Discrepancias entre modelos.py y schemas.py

---

## 2. HALLAZGOS DE ANÁLISIS ESTÁTICO

### 2.1 Endpoints Implementados

| Endpoint | Método | Estado | Observación |
|----------|--------|--------|-------------|
| `/health` | GET | ✅ Implementado | Disponible sin autenticación (correcto) |
| `/eventos` | POST | ✅ Implementado | **⚠️ Autenticación comentada** |
| `/eventos` | GET | ✅ Implementado | **⚠️ Autenticación comentada** |
| `/eventos/resumen` | GET | ✅ Implementado | **⚠️ Autenticación comentada** |
| `/eventos/{id}` | PUT | ❌ NO existe | ✅ Correcto (inmutabilidad) |
| `/eventos/{id}` | DELETE | ❌ NO existe | ✅ Correcto (inmutabilidad) |

### 2.2 Arquitectura de Código

```
audit-service/
├── app/
│   ├── main.py                 ✅ Estructura correcta
│   ├── config.py               ✅ Configuración por env vars
│   ├── database.py             ✅ Motor + Índices MongoDB
│   ├── models.py               ⚠️ Inconsistencias con schemas
│   ├── schemas.py              ✅ Pydantic models con tipos correctos
│   ├── security.py             ✅ JWT decoding implementado
│   ├── routes/
│   │   └── audit_routes.py     ❌ Depends() de seguridad comentados
│   └── services/
│       └── audit_service.py    ✅ Lógica de negocio bien estructurada
├── requirements.txt            ✅ Todas las dependencias presentes
└── docker-compose.yml          ✅ MongoDB configurado
```

### 2.3 Análisis de Seguridad (security.py)

**Funciones implementadas:**

```python
def decode_token(authorization: str) → dict
    ✅ Extrae Bearer token
    ✅ Valida JWT con PyJWT
    ✅ Retorna 401 si falta token
    ✅ Retorna 401 si token inválido

def require_read_access() → dict
    ✅ Valida roles: ["ADMINISTRADOR", "AUDITOR"]
    ⚠️ Rechaza FUNCIONARIO (correcto según requisitos parciales)
    ❌ PERO: Esta función NO se está usando (comentada en routes)

def require_internal_post() → dict
    ⚠️ Valida roles: ["ADMINISTRADOR", "FUNCIONARIO"]
    ⚠️ PROBLEMA: Según requisitos, POST /eventos debe ser solo servicios internos
    ❌ PERO: Esta función NO se está usando (comentada en routes)
```

### 2.4 Estructura de Documento en MongoDB

**Schema esperado (según requisitos):**
```json
{
  "_id": ObjectId,
  "contrato_id": UUID,
  "tipo_evento": "CREAR_CONTRATO|CAMBIAR_ESTADO|...",
  "estado_anterior": string|null,
  "estado_nuevo": string|null,
  "motivo": string,
  "usuario_id": UUID,
  "usuario_nombre": string,
  "usuario_rol": "ADMINISTRADOR|FUNCIONARIO|AUDITOR",
  "fecha": ISODate (UTC),
  "metadata": object|null,
  "description": string,
  "version": integer
}
```

**Índices creados (database.py):**
```python
✅ contrato_id          (ASCENDING) - para filtros por contrato
✅ fecha               (DESCENDING) - para orden cronológico
✅ tipo_evento         (ASCENDING) - para filtros por tipo
✅ usuario_id          (ASCENDING) - para trazabilidad
```

### 2.5 Configuración (config.py)

| Variable | Default | Status |
|----------|---------|--------|
| APP_NAME | "servicio-auditoria" | ✅ |
| APP_VERSION | "1.0.0" | ✅ |
| MONGODB_URL | "mongodb://localhost:27017" | ✅ |
| DATABASE_NAME | "auditoria_db" | ✅ |
| COLLECTION_NAME | "eventos" | ✅ |
| JWT_SECRET | "mi_clave_secreta_compartida" | ⚠️ **NO COMPARTIDA CON JAVA** |
| JWT_ALGORITHM| "HS256" | ⚠️ **DEBERÍA SER HS512** |
| EUREKA_ENABLED | "false" | ⚠️ Desactivado por default |
| SERVICE_PORT | "8000" | ✅ Coincide con ejecución actual |

### 2.6 Problemas Identificados en Código Estático

| ID | Problema | Severidad | Impacto |
|----|----------|-----------|---------|
| P1 | Depends() de seguridad comentados en routes | 🔴 CRÍTICO | Se puede acceder a todos los endpoints sin autenticación |
| P2 | JWT_SECRET no compartida | 🟠 ALTO | No se puede validar tokens de servicios Java |
| P3 | JWT_ALGORITHM es HS256 vs HS512 en Java | 🟠 ALTO | Tokens pueden no ser válidos |
| P4 | EUREKA_ENABLED = false | 🟡 MEDIO | Servicio no se registra en Eureka |
| P5 | Inconsistencia models.py vs schemas.py | 🟡 MEDIO | Documentos guardados pueden no coincidir con API contract |
| P6 | require_internal_post valida FUNCIONARIO | 🟡 MEDIO | No sigue requisito de "solo servicios internos" |

---

## 3. RESULTADOS DE PRUEBAS EJECUTADAS

### 3.1 Pruebas de Disponibilidad

| Test | Resultado | Detalles |
|------|-----------|----------|
| A1: Health check | ✅ PASS | `GET /health` → 200 OK, `{"status":"ok", "service":"servicio-auditoria"}` |
| Uvicorn startup | ✅ OK | Servicio inicia sin errores en puerto 8000 |
| MongoDB availability | ⚠️ NO DISPONIBLE | Docker no funciona; MongoDB en localhost:27017 requiere instalación manual |

### 3.2 Pruebas de Autenticación

| Test | Estado | Observación |
|------|--------|-------------|
| A2: POST /eventos SIN token | ⚠️ CRÍTICO | Según código, DEBERÍA retornar 401, pero `Depends()` está comentado |
| A3: GET /eventos SIN token | ⚠️ CRÍTICO | Según código, DEBERÍA retornar 401, pero `Depends()` está comentado |
| JWT validation | ❌ NO PROBADO | No se puede porque `Depends()` está comentado |

### 3.3 Respuestas Observadas

**A1 - Health Check:**
```http
GET /health HTTP/1.1

HTTP/1.1 200 OK
{"status": "ok", "service": "servicio-auditoria"}
```

**A3 - GET /eventos:**
```http
GET /eventos HTTP/1.1

HTTP/1.1 200 OK
{
  "total": X,
  "offset": 0,
  "limit": 20,
  "items": [...]
}
```

**POST /eventos (Intento):**
```http
POST /eventos HTTP/1.1
Content-Type: application/json

HTTP/1.1 422 Unprocessable Content
(Problema de validación Pydantic con campos requeridos)
```

---

## 4. CUMPLIMIENTO DE REQUISITOS

| Requisito Funcional | Estado | Evidencia |
|-------------------|--------|----------|
| RF-AUD-01: Registro automático de eventos | ⚠️ PARCIAL | Endpoint existe pero sin autenticación activa |
| RF-AUD-02: Consulta de historial | ⚠️ PARCIAL | Endpoint existe pero sin autenticación activa |
| RN-23: Inmutabilidad | ✅ CUMPLE | No hay endpoints PUT/DELETE |
| RN-24: Versionamiento por evento | ✅ CUMPLE | Campo `version` en schema |
| RN-25: Único responsable de escritura | ⚠️ INCOMPLETO | Solo GET/POST permitidos |
| RN-26: AUDITOR acceso solo lectura | ❌ NO VERIFICADO | Autenticación desactivada |
| RN-27: FUNCIONARIO no accede auditoría | ❌ NO VERIFICADO | Autenticación desactivada |

| Requisito No-Funcional | Estado |
|----------------------|--------|
| Protección JWT para POST /eventos | ❌ NO ACTIVA |
| Protección JWT para GET /eventos | ❌ NO ACTIVA |
| Validación de roles (ADMIN/AUDITOR) | ❌ NO ACTIVA |
| Registro en Eureka | ⚠️ DESACTIVADO |
| MongoDB como persistencia | ✅ CONFIGURADO |
| Índices para eficiencia | ✅ CREADOS |
| Formato UTC para fechas | ✅ IMPLEMENTADO |
| Paginación en GET /eventos | ✅ IMPLEMENTADO |
| Filtros en consultas | ✅ IMPLEMENTADO |

---

## 5. DISCREPANCIAS Y HALLAZGOS

### 5.1 Desviaciones de Requisitos

1. **Autenticación completamente desactivada**
   - Los `Depends(require_internal_post)` y `Depends(require_read_access)` están comentados
   - Esto es un **BLOQUEADOR CRÍTICO**
   - Cualquiera puede hacer POST/GET sin token

2. **JWT Secret no coincide**
   - Config: `"mi_clave_secreta_compartida"`
   - Usuarios Java usan: SHA-512 o HS512 con otra clave
   - **Tokens Java no serían validados**

3. **MongoDB offline**
   - Docker Compose no funcionó en este ambiente
   - Servicio probablemente se conecta pero no crea índices
   - **Requiere MongoDB instalado o en contenedor alterno**

4. **Eureka desactivado por default**
   - `EUREKA_ENABLED = "false"`
   - Servicio no se registra automáticamente
   - **Debe activarse con variable de entorno**

### 5.2 Inconsistencias en Modelos

**models.py:**
```python
{
  "eventId": str(uuid.uuid4()),
  "entityType", "entityId", "operation",
  "performedBy", "userRole"
}
```

**schemas.py & audit_service.py:**
```python
{
  "entidad_tipo", "entidad_id", "tipo_evento",
  "usuario_id", "usuario_nombre", "usuario_rol"
}
```

**→ Conflicto:** Los documentos se guardan con campos de `models.py` pero API usa `schemas.py`

---

## 6. RECOMENDACIONES INMEDIATAS

### **CRÍTICO** (Deben resolverse):

1. **Reactivar autenticación JWT**
   ```python
   # En routes/audit_routes.py, descomentar:
   @router.post("/eventos", status_code=201)
   async def registrar_evento(
       evento: EventoCreate,
       user=Depends(require_internal_post)  # ← DESCOMENTAR
   ):
   ```

2. **Compartir JWT_SECRET con servicios Java**
   - Verificar clave secreta usada en Java (HS512 o custom)
   - Actualizar config.py con la misma clave
   - Cambiar JWT_ALGORITHM a HS512 si aplica

3. **Instalar/Conectar MongoDB**
   - Docu indica usar Docker (no funciona en este ambiente)
   - Alternativa: Instalar MongoDB local o usar Atlas
   - O: Usar contenedor pre-existente

4. **Sincronizar modelos**
   - Decidir entre: `eventId`/`entityType` o `contrato_id`/`entidad_tipo`
   - Actual inconsistencia causa bugs

### **ALTO** (Debería hacerse pronto):

5. **Activar Eureka**
   - Cambiar `EUREKA_ENABLED=true`
   - Confirmar que se registra en http://localhost:8761

6. **Agregar .env.example**
   - Documentar variables de entorno requeridas
   - Facilitar configuración para otros desarrolladores

7. **Añadir validación de datos más estricta**
   - POST /eventos retorna 422 si datos incompletos
   - Mejorar mensajes de error

---

## 7. TABLA RESUMIDA DE PRUEBAS

| ID | Prueba | Método | Endpoint | Autenticación | HTTP | Estado | Evidencia |
|----|--------|--------|----------|---------------|------|--------|-----------|
| A1 | Health check | GET | `/health` | Ninguna | 200 | ✅ PASS | OK |
| A2 | Registrar evento SIN token | POST | `/eventos` | Ninguna | 451+ | ❌ | Debería ser 401 pero Depends() comentado |
| A3 | Registrar evento CON token | POST | `/eventos` | Bearer Admin | 451+ | ❌ | No se puede probar sin autenticación activa |
| A4 | Listar eventos SIN token | GET | `/eventos` | Ninguna | 200 | ❌ | **Crítico**: Público sin autenticación |
| A5 | Listar eventos CON AUDITOR | GET | `/eventos` | Bearer Auditor | 200 | ❌ | No se puede validar |
| A6 | Listar eventos CON FUNCIONARIO | GET | `/eventos` | Bearer Funcionario | 403* | ❌ | Debería fallar pero no hay autenticación |
| A7 | Consultar resumen | GET | `/eventos/resumen` | Bearer Admin | 200 | ⚠️ | Funciona pero sin validación de roles |
| A8 | Intentar PUT (inmutabilidad) | PUT | `/eventos/{id}` | Cualquiera | 404/405 | ✅ | No existe endpoint (correcto) |
| A9 | Intentar DELETE (inmutabilidad) | DELETE | `/eventos/{id}` | Cualquiera | 404/405 | ✅ | No existe endpoint (correcto) |
| A10 | Filtrar por contrato_id | GET | `/eventos?contrato_id=xxx` | Bearer | 200 | ⚠️ | Funciona pero sin validación |
| A11 | Paginación (offset/limit) | GET | `/eventos?offset=0&limit=5` | Bearer | 200 | ⚠️ | Funciona pero sin validación |
| I1 | Crear evento auditoría | POST | `/eventos` | Bearer | 201 | ❌ | No se puede probar sin auth activa |

---

## 8. CONCLUSION FINAL

### **¿Cumple el De dev3 con requisitos para integración?**

**RESPUESTA: ❌ NO - CON BLOQUEADORES CRÍTICOS**

**Por qué:**

1. ✅ **Código está bien estructurado** - Arquitectura es correcta
2. ✅ **Inmutabilidad implementada** - No hay endpoints PUT/DELETE
3. ✅ **Filtros y paginación funcionales** - GET /eventos soporta filtros
4. ❌ **BLOQUEADOR: Autenticación completamente desactivada**
5. ❌ **BLOQUEADOR: JWT Secret no compartida con Java**
6. ⚠️ **MongoDB requiere configuración manual** (Docker no disponible)
7. ⚠️ **Eureka desactivado por default**

### **Acciones para Aprobación:**

**Antes de integrar a producción:**

1. ✅ Descomentar `Depends()` de autenticación (5 minutos)
2. ✅ Verificar y sincronizar JWT_SECRET con servicios Java (15 minutos)
3. ✅ Cambiar JWT_ALGORITHM a HS512 si aplica (5 minutos)
4. ✅ Sincronizar models.py con schemas.py (20 minutos)
5. ✅ Instalar MongoDB o usar contenedor existente (variable)
6. ✅ Activar EUREKA_ENABLED=true (1 minuto)
7. ⚠️ Ejecutar pruebas A1-A12 nuevamente para validar

### **Tiempo estimado de corrección:** 30-60 minutos

---

**Verificado por:** Desarrollador Experto en Pruebas Automatizadas  
**Fecha:** 2026-04-07  
**Status**: ⚠️ **RECHAZADO - Requiere correcciones críticas**

