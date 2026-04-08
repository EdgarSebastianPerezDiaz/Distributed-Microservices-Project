# 📋 GUÍA DE EJECUCIÓN MANUAL - SISTEMA DISTRIBUIDO DE MICROSERVICIOS

**Fecha:** 8 de Abril de 2026  
**Versión:** 1.0  
**Plataforma:** Windows 10/11 (CMD o PowerShell)

---

## ⚡ REQUISITOS PREVIOS

Antes de iniciar los servicios, verifica que tu sistema tenga instalado:

### Requerido:
- ✅ **Java JDK 17** - Verifica con: `java -version`
- ✅ **Apache Maven 3.8+** - Verifica con: `mvn -version`
- ✅ **Python 3.9+** (solo para audit-service) - Verifica con: `python --version`
- ✅ **PostgreSQL 13+** (Base de datos para 5 servicios) - Verifica con: `psql --version`
- ✅ **MongoDB 5+** (Base de datos para auditoría) - Verifica con: `mongo --version` o `mongod --version`

### Recomendado:
- 📌 **Git Bash** o **PowerShell** (mejor que CMD)
- 📌 **Tabbed Terminal** (ej. Windows Terminal para manejar múltiples tabs)
- 📌 **curl** o **Postman** (para verificar endpoints)
- 📌 **Docker** (opcional, para PostgreSQL/MongoDB en contenedores)

### Verificación Rápida:
```powershell
java -version
mvn -version
python --version
psql --version
```

---

## 🗂️ ESTRUCTURA DEL PROYECTO

```
C:\Users\DELL\Downloads\Distributed-Microservices-Project\
├── eureka-server\eureka-server\          (Registro de servicios)
├── api-gateway\api-gateway\              (Puerta de entrada)
├── usuario-service\usuario-service\      (Gestión de usuarios)
├── proveedor-service\proveedor-service\  (Gestión de proveedores)
├── contrato-service\contrato-service\    (Gestión de contratos)
└── audit-service\                        (Auditoría - Python)
```

---

## 🚀 ORDEN DE INICIO (CRÍTICO)

```
1️⃣  EUREKA SERVER      (Puerto 8761) - Primero, es el registry
2️⃣  API GATEWAY        (Puerto 8081) - Segundo, necesita Eureka
3️⃣  USUARIO SERVICE    (Puerto 8084) - Tercero, autenticación
4️⃣  PROVEEDOR SERVICE  (Puerto 8082) - Cuarto
5️⃣  CONTRATO SERVICE   (Puerto 8083) - Quinto
6️⃣  AUDIT SERVICE      (Puerto 8000) - Último - Python
```

⚠️ **IMPORTANTE:** Cada servicio debe iniciar CORRECTAMENTE antes de pasar al siguiente.

---

## 📖 INSTRUCCIONES POR SERVICIO

### ═══════════════════════════════════════════════════════════════
### 1️⃣ EUREKA SERVER
### ═══════════════════════════════════════════════════════════════

**Propósito:** Servidor de registro de servicios - actúa como directorio central.

#### Paso 1: Navegar a la carpeta del servicio
```powershell
cd C:\Users\DELL\Downloads\Distributed-Microservices-Project\eureka-server\eureka-server
```

#### Paso 2: Compilar (primera vez solamente)
```powershell
mvn clean install
```
⏱️ Espera: 2-3 minutos (descargará dependencias)

#### Paso 3: Ejecutar el servicio
```powershell
mvn spring-boot:run
```

#### Verificación de Inicio:
- **Indicador en terminal:** Deberías ver mensaje `Eureka Server started`
- **Puerto:** `http://localhost:8761`
- **Abre en navegador:** `http://localhost:8761`
  - Si ves dashboard Eureka con "DS Replicas" = ✅ FUNCIONAL

#### Logs esperados (últimas líneas):
```
... Tomcat started on port(s): 8761 (http) with context path ''
... Started EurekaServerApplication in X seconds (JVM running for X seconds)
```

---

### ═══════════════════════════════════════════════════════════════
### 2️⃣ API GATEWAY
### ═══════════════════════════════════════════════════════════════

**Propósito:** Puerta de entrada única - enruta requests a todos los servicios.

#### Paso 1: ABRE UNA NUEVA TERMINAL PowerShell (NO cierres la de Eureka)

#### Paso 2: Navegar a la carpeta
```powershell
cd C:\Users\DELL\Downloads\Distributed-Microservices-Project\api-gateway\api-gateway
```

#### Paso 3: Compilar (primera vez)
```powershell
mvn clean install
```
⏱️ Espera: 1-2 minutos

#### Paso 4: Ejecutar el servicio
```powershell
mvn spring-boot:run
```

#### Verificación de Inicio:
- **Terminal:** Mensaje `Netty started on port(s): 8081 with context path ''`
- **Puerto:** `http://localhost:8081`
- **Verifica en navegador:** `http://localhost:8081/actuator/health`
  - Deberías ver JSON: `{"status":"UP"}`

#### Verificación en Eureka:
- Ve a `http://localhost:8761` en navegador
- Deberías ver "API-GATEWAY" registrada en rojo o naranja (está bien mientras aparezca)

---

### ═══════════════════════════════════════════════════════════════
### 3️⃣ USUARIO SERVICE (Autenticación)
### ═══════════════════════════════════════════════════════════════

**Propósito:** Gestión de usuarios y autenticación JWT.

#### Paso 1: ABRE NUEVA TERMINAL (NO cierres Eureka ni Gateway)

#### Paso 2: Navegar a la carpeta
```powershell
cd C:\Users\DELL\Downloads\Distributed-Microservices-Project\usuario-service\usuario-service
```

#### Paso 3: Compilar
```powershell
mvn clean install
```
⏱️ Espera: 2 minutos

#### Paso 4: Ejecutar
```powershell
mvn spring-boot:run
```

#### Verificación de Inicio:
- **Terminal:** `Tomcat started on port(s): 8084`
- **Health Check:** `http://localhost:8084/actuator/health`
  - JSON esperado: `{"status":"UP","components":{"db":{"status":"UP"},...}}`
  
#### Verificación en Eureka:
- Ve a `http://localhost:8761`
- Deberías ver "USUARIO-SERVICE" registrado

#### Próxima acción:
- Si ves en Eureka que USUARIO-SERVICE está en **AZUL**, ¡está listo!
- Si está en ROJO/NARANJA, espera 30 segundos y refresca

---

### ═══════════════════════════════════════════════════════════════
### 4️⃣ PROVEEDOR SERVICE
### ═══════════════════════════════════════════════════════════════

**Propósito:** Gestión de proveedores.

#### Paso 1: ABRE NUEVA TERMINAL

#### Paso 2: Navegar
```powershell
cd C:\Users\DELL\Downloads\Distributed-Microservices-Project\proveedor-service\proveedor-service
```

#### Paso 3: Compilar
```powershell
mvn clean install
```
⏱️ Espera: 2 minutos

#### Paso 4: Ejecutar
```powershell
mvn spring-boot:run
```

#### Verificación:
- **Terminal:** `Tomcat started on port(s): 8082`
- **Health:** `http://localhost:8082/actuator/health` → `{"status":"UP"}`
- **Eureka Dashboard:** Ver "PROVEEDOR-SERVICE" registrado

---

### ═══════════════════════════════════════════════════════════════
### 5️⃣ CONTRATO SERVICE
### ═══════════════════════════════════════════════════════════════

**Propósito:** Gestión de contratos (con auditoría integrada).

#### Paso 1: ABRE NUEVA TERMINAL

#### Paso 2: Navegar
```powershell
cd C:\Users\DELL\Downloads\Distributed-Microservices-Project\contrato-service\contrato-service
```

#### Paso 3: Compilar
```powershell
mvn clean install
```
⏱️ Espera: 2 minutos

#### Paso 4: Ejecutar
```powershell
mvn spring-boot:run
```

#### Verificación:
- **Terminal:** `Tomcat started on port(s): 8083`
- **Health:** `http://localhost:8083/actuator/health` → `{"status":"UP"}`
- **Eureka:** CONTRATO-SERVICE registrado

---

### ═══════════════════════════════════════════════════════════════
### 6️⃣ AUDIT SERVICE (Python)
### ═══════════════════════════════════════════════════════════════

**Propósito:** Servicio de auditoría - registra todos eventos del sistema.

#### Paso 1: ABRE NUEVA TERMINAL

#### Paso 2: Navegar
```powershell
cd C:\Users\DELL\Downloads\Distributed-Microservices-Project\audit-service
```

#### Paso 3: Crear entorno virtual Python (primera vez solamente)
```powershell
python -m venv venv
```
⏱️ Espera: 1 minuto

#### Paso 4: Activar entorno virtual
```powershell
venv\Scripts\Activate.ps1
```
✅ Deberías ver `(venv)` al inicio de tu terminal

#### Paso 5: Instalar dependencias
```powershell
pip install -r requirements.txt
```
⏱️ Espera: 2-3 minutos (instala FastAPI, PyJWT, MongoDB, etc.)

#### Paso 6: Verificar variables de entorno (crear archivo `.env`)

En PowerShell, crea el archivo `.env` en la carpeta audit-service:
```powershell
# Desde dentro de C:\Users\DELL\Downloads\Distributed-Microservices-Project\audit-service
echo "EUREKA_ENABLED=true" > .env
echo "EUREKA_SERVICE_NAME=AUDITORIA-SERVICE" >> .env
echo "EUREKA_SERVER_URL=http://localhost:8761" >> .env
echo "MONGODB_URI=mongodb://localhost:27017" >> .env
echo "JWT_SECRET=tu_clave_secreta_jwt_128_caracteres_aqui" >> .env
```

O crear manualmente archivo `.env` con contenido:
```
EUREKA_ENABLED=true
EUREKA_SERVICE_NAME=AUDITORIA-SERVICE
EUREKA_SERVER_URL=http://localhost:8761
MONGODB_URI=mongodb://localhost:27017
JWT_SECRET=tu_clave_secreta_jwt_128_caracteres_aqui
```

#### Paso 7: Ejecutar el servicio
```powershell
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

#### Verificación:
- **Terminal:** `Uvicorn running on http://127.0.0.1:8000`
- **Health Check:** `http://localhost:8000/health`
  - JSON esperado: `{"status":"ok"}`
- **Eureka Dashboard:** Deberías ver "AUDITORIA-SERVICE" registrado

---

## ✅ VERIFICACIÓN COMPLETA DEL SISTEMA

Una vez que todos los servicios están corriendo, ejecuta estos pasos:

### 1️⃣ Verificar Eureka Dashboard
```
Abre en navegador: http://localhost:8761
```
**Deberías ver 6 servicios registrados:**
```
Application     AMIs     Availability Zones
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
API-GATEWAY     1        1
USUARIO-SERVICE 1        1
PROVEEDOR-SERVICE 1      1
CONTRATO-SERVICE 1       1
AUDITORIA-SERVICE 1      1
(Eureka Server se registra a sí mismo)
```

✅ Si ves esto, todos los servicios se han registrado correctamente.

### 2️⃣ Verificar Health Checks Individuales

En una terminal (PowerShell o CMD), ejecuta:
```powershell
# Eureka
curl http://localhost:8761
# Resultado: Dashboard HTML

# API Gateway
curl http://localhost:8081/actuator/health
# Resultado: {"status":"UP"}

# Usuario Service
curl http://localhost:8084/actuator/health
# Resultado: {"status":"UP","components":{"db":{"status":"UP"},...}}

# Proveedor Service
curl http://localhost:8082/actuator/health
# Resultado: {"status":"UP"}

# Contrato Service
curl http://localhost:8083/actuator/health
# Resultado: {"status":"UP"}

# Audit Service
curl http://localhost:8000/health
# Resultado: {"status":"ok"}
```

### 3️⃣ Verificar Bases de Datos

#### PostgreSQL (5 servicios):
```powershell
psql -U postgres -h localhost -c "\l"
```
Deberías ver bases de datos:
- `usuario_service_db`
- `proveedor_service_db`
- `contrato_service_db`
- (Y otras...)

#### MongoDB (Auditoría):
```powershell
mongo localhost:27017
db.adminCommand('ping')
```
Resultado: `{ "ok" : 1 }`

---

## 🔗 CONFIGURACIÓN COMPARTIDA

### JWT Secret (CRÍTICO - DEBE SER IDÉNTICO)

El JWT secret está configurado en:
- ✅ `usuario-service/src/main/resources/application.yaml`
- ✅ `proveedor-service/src/main/resources/application.yaml`
- ✅ `contrato-service/src/main/resources/application.yaml`
- ✅ `audit-service/.env`

**Valor actual (ejemplo):**
```
jwt.secret=tu_clave_secreta_jwt_128_caracteres_aqui_12345678901234567890
```

⚠️ **Si cambias este valor, DEBE ser idéntico en todos los sitios.**

---

## 🛠️ SOLUCIÓN DE PROBLEMAS

### ❌ Error: "Port 8761 is already in use"
```powershell
# Encuentra qué proceso usa el puerto
netstat -ano | findstr ":8761"

# Termina el proceso (reemplaza PID con el número)
taskkill /PID <PID> /F

# O, simplemente, cambia el puerto en application.yaml
# Búsca: server.port=8761
# Cambia a: server.port=8762
```

### ❌ Error: "Cannot connect to database"
```powershell
# Verifica que PostgreSQL esté corriendo
psql -U postgres -h localhost -c "SELECT 1"

# Si no funciona, inicia PostgreSQL:
# En Windows: Abre Services (services.msc) y busca PostgreSQL, dale Start
```

### ❌ Error: "MongoDB connection refused"
```powershell
# Verifica que MongoDB está corriendo
mongo localhost:27017

# Si no funciona, inicia MongoDB:
# En Windows: Abre Services (services.msc) y busca MongoDB, dale Start
# O ejecuta: mongod
```

### ❌ Error: "Service not registered in Eureka"
- Espera 30 segundos (registración es asincrónica)
- Verifica que el nombre en `spring.application.name` en `application.yaml` está correcto
- Verifica que `eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/` es correcto

### ❌ Error: "UNAUTHORIZED" en requests JWT
- Verifica que el JWT secret es idéntico en todos los servicios
- Verifica que el token JWT tiene formato: `Authorization: Bearer <token>`
- Verifica que el token no ha expirado

### ❌ Error: Maven "Cannot download dependencies"
```powershell
# Limpia caché local de Maven
mvn clean

# O, elimina carpeta .m2
rm -r $env:USERPROFILE\.m2\repository

# Y vuelve a intentar
mvn clean install
```

---

## 📊 RESUMEN - TABLA DE PUERTOS Y URLS

| Servicio | Puerto | URL | Health | Eureka |
|----------|--------|-----|--------|--------|
| **Eureka Server** | 8761 | http://localhost:8761 | (dashboard) | - |
| **API Gateway** | 8081 | http://localhost:8081 | /actuator/health | ✅ |
| **Usuario Service** | 8084 | http://localhost:8084 | /actuator/health | ✅ |
| **Proveedor Service** | 8082 | http://localhost:8082 | /actuator/health | ✅ |
| **Contrato Service** | 8083 | http://localhost:8083 | /actuator/health | ✅ |
| **Audit Service** | 8000 | http://localhost:8000 | /health | ✅ |

---

## 🎯 CHECKLIST DE INICIO COMPLETO

- [ ] Requisitos previos instalados (Java, Maven, Python, PostgreSQL, MongoDB)
- [ ] Terminal 1: Eureka Server iniciado en puerto 8761 ✅
- [ ] Terminal 2: API Gateway iniciado en puerto 8081 ✅
- [ ] Terminal 3: Usuario Service iniciado en puerto 8084 ✅
- [ ] Terminal 4: Proveedor Service iniciado en puerto 8082 ✅
- [ ] Terminal 5: Contrato Service iniciado en puerto 8083 ✅
- [ ] Terminal 6: Audit Service iniciado en puerto 8000 ✅
- [ ] Eureka Dashboard muestra 6 servicios: http://localhost:8761 ✅
- [ ] Todos los health checks retornan status UP/ok
- [ ] PostgreSQL accesible y bases de datos creadas
- [ ] MongoDB accesible y conectado

---

## 📞 SIGUIENTES PASOS

Una vez que todos los servicios estén funcionando:

1. **Prueba Login (Crear Usuario):**
   ```powershell
   curl -X POST http://localhost:8081/api/auth/register `
     -H "Content-Type: application/json" `
     -d '{"email":"test@example.com","password":"password123","role":"FUNCIONARIO"}'
   ```

2. **Obtén JWT Token:**
   ```powershell
   curl -X POST http://localhost:8081/api/auth/login `
     -H "Content-Type: application/json" `
     -d '{"email":"test@example.com","password":"password123"}'
   ```

3. **Crea Proveedor (requiere ADMIN):**
   ```powershell
   curl -X POST http://localhost:8081/api/suppliers `
     -H "Authorization: Bearer <TOKEN_JWT>" `
     -H "Content-Type: application/json" `
     -d '{...proveedorData...}'
   ```

4. **Crea Contrato (requiere FUNCIONARIO):**
   ```powershell
   curl -X POST http://localhost:8081/api/contracts `
     -H "Authorization: Bearer <TOKEN_JWT>" `
     -H "Content-Type: application/json" `
     -d '{...contratoData...}'
   ```

---

**✅ Guía completa. ¡Listo para ejecutar!**

Cualquier duda, revisa los logs en cada terminal para ver mensajes de error específicos.
