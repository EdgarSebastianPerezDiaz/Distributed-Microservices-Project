# Historial Completo de Cambios - Proyecto Microservicios Distribuidos

## Resumen General
Este documento contiene un registro completo de todos los cambios realizados en el proyecto desde su inicio (24 de marzo de 2026) hasta la fecha actual (18 de abril de 2026).

**Total de commits:** 56 commits
**Período:** 24 de marzo - 18 de abril de 2026
**Ramas principales:** main, dev1-infra-users, dev2-providers-contracts, dev3-audit

---

## Cambios Recientes (Última Sesión - 18 de abril de 2026)

### ✅ Commit: 8ae7b62 - "Correct yaml in proveedor-service and contrato-service"
**Autor:** Braya Yesid Holguin Zorro | **Fecha:** 2026-04-18

**Archivos modificados:**
- `contrato-service/src/main/resources/application.yaml` (8 cambios: +8 insertions, -3 deletions)
- `proveedor-service/src/main/resources/application.yaml` (5 cambios: +5 insertions, -1 deletions)
- `distribuidos/proveedor_service/service/SupplierService.java` (3 cambios: +3 insertions)

**Descripción:** Correcciones en los archivos YAML de configuración de los servicios de proveedores y contratos.

---

### ✅ Commit: 82b2891 - "collection de endpoints generales del proyecto"
**Autor:** Andres | **Fecha:** 2026-04-18

**Archivos modificados:**
- `postman/Proyecto.postman_collection.json` (509 cambios: +509 insertions)

**Descripción:** Se añadió colección completa de Postman con todos los endpoints del proyecto.

---

### ✅ Commit: 2e2c9c4 - "Añadido funcionalidad de editar usuario junto con su registro en auditoria"
**Autor:** Andres | **Fecha:** 2026-04-18

**Archivos modificados:**
- `usuario_service/controller/AuthController.java` (+12 lines)
- `usuario_service/dto/UserUpdateRequest.java` (NUEVO - +28 lines)
- `usuario_service/service/UserService.java` (+72 lines)

**Descripción:** Implementación de funcionalidad para editar usuarios con registro automático en auditoría.

---

### ✅ Commit: 7354984 - "User Audit"
**Autor:** Andres | **Fecha:** 2026-04-18

**Archivos modificados:**
- `usuario_service/client/AuditClient.java` (NUEVO - +43 lines)
- `usuario_service/config/SecurityConfig.java` (+7 lines)
- `usuario_service/controller/AuthController.java` (+10 lines)
- `usuario_service/dto/AuditEventDTO.java` (NUEVO - +30 lines)
- `usuario_service/security/JwtService.java` (+5 lines)
- `usuario_service/service/UserService.java` (+97 lines, -13 lines)

**Descripción:** Integración de auditoría en el servicio de usuarios - tracking de eventos y cambios de usuarios.

---

### ✅ Commit: 07ddb85 - "audit de proveedor"
**Autor:** Andres | **Fecha:** 2026-04-18

**Archivos modificados:**
- `audit-service/app/schemas.py` (+1 line)
- `contrato_service/service/ContractService.java` (+21 lines)
- `proveedor_service/service/SupplierService.java` (+55 lines, -4 lines)

**Descripción:** Añadida integración de auditoría para el servicio de proveedores.

---

### ✅ Commit: 3c1858e - "auditoria config"
**Autor:** Andres | **Fecha:** 2026-04-18

**Cambios principales en auditoría y configuración:**
- `contrato_service/client/AuditClient.java` (modificado)
- `contrato_service/dto/EventoAuditoriaDTO.java` (actualizado)
- `proveedor_service/client/AuditClient.java` (mejorado)
- `proveedor_service/dto/AuditEventDTO.java` (actualizado)
- Filtros JWT actualizados en ambos servicios

**Descripción:** Configuración completa de auditoría para servicios de contratos y proveedores.

---

## Cambios Anteriores (15-16 de abril de 2026)

### ✅ Commit: d6e01ea - "m"
**Autor:** Andres | **Fecha:** 2026-04-16
- `audit-service/app/schemas.py` (ajustes menores)

### ✅ Commit: 607f3b7 - "m"
**Autor:** Andres | **Fecha:** 2026-04-16
- Cambios en configuración de audit-service
- Ajustes en modelos y esquemas

### ✅ Commit: 04ab1d8 - "ajustes"
**Autor:** Andres | **Fecha:** 2026-04-15
- Ajustes en auditoría y DTOs
- Cambios en ContractService y SupplierService

---

## Cambios en Rama dev2-providers-contracts (9 de abril de 2026)

### ✅ Commit: 78bfb29 - "Add Postman collection for Dev2 - Proveedores y Contratos tests"
**Autor:** Braya Yesid Holguin Zorro | **Fecha:** 2026-04-09

**Archivos modificados:**
- `postman/dev2-postman-collection.json` (730 insertions)

**Descripción:** Colección Postman completa para testing de servicios de Proveedores y Contratos.

---

### ✅ Commit: dd89826 - "[US-206] Implement 6-state contract machine and supplier audit"
**Autor:** Braya Yesid Holguin Zorro | **Fecha:** 2026-04-09

**Cambios principales:**
- Implementación de máquina de estados de 6 estados para contratos
- Integración de auditoría para proveedores
- Correcciones en base de datos
- 26 archivos modificados (677 insertions, 358 deletions)

**Servicios afectados:**
- **contrato-service:** Refactoring de ContractService y ContractStateMachine
- **proveedor-service:** Nuevo cliente de auditoría, mappers actualizados, servicios mejorados

---

## Cambios de Fusiones y Reversiones (8 de abril de 2026)

### ⚠️ Commit: cc32cdd - "Merge branch 'main'"
**Autor:** Braya Yesid Holguin Zorro | **Fecha:** 2026-04-08
- Merge de cambios principales

### ↩️ Commit: e7c04ab - "Revert [US-206]"
**Autor:** Braya Yesid Holguin Zorro | **Fecha:** 2026-04-08
- Reversión de cambios de máquina de estados de contrato

### ↩️ Commit: 3b9c22e - "Revert [US-206]"
**Autor:** Braya Yesid Holguin Zorro | **Fecha:** 2026-04-08
- Reversión adicional de cambios

---

## Rama dev1-infra-users (8 de abril de 2026)

### ✅ Commit: 5d9ef73 - "comentarios"
**Autor:** Lina Xiomara Ladino Fernandez | **Fecha:** 2026-04-08

**Cambios principales:**
- Actualización de comentarios en ApiGateway
- Mejoras en JwtAuthenticationFilter
- Actualización de EurekaServerApplication
- Configuración mejorada en UsuarioService
- Nuevas pruebas unitarias (12 archivos, 710 insertions)

**Nuevas clases de prueba:**
- `DataInitializerTest.java`
- `SecurityConfigTest.java`
- `JwtAuthenticationFilterTest.java`
- `AuthControllerTest.java`

---

### ✅ Commit: a91d193 - "ajuste de conexion"
**Autor:** Andres | **Fecha:** 2026-04-08
- Ajustes en SecurityConfig para conectividad

### ✅ Commit: 89752e5 - "[US-206] Implement 6-state contract machine"
**Autor:** Braya Yesid Holguin Zorro | **Fecha:** 2026-04-08
- Cambios en ContractService

### ✅ Commit: b849fae - "collection"
**Autor:** Andres | **Fecha:** 2026-04-08
- `postman/audit-service.postman_collection.json` (+127 lines)
- Colección de endpoints para audit-service

### ✅ Commit: c7d30f2 - "[US-206] Implement 6-state contract machine and supplier audit"
**Autor:** Braya Yesid Holguin Zorro | **Fecha:** 2026-04-08
- 12 archivos modificados (336 insertions, 195 deletions)

---

## Cambios del Gatekeeping y Consolidación de Sprint 1 (8 de abril de 2026)

### ✅ Commit: 36bcde6 - "chore: Actualizar .gitignore para Maven e IDE"
**Autor:** Sebastian Perez | **Fecha:** 2026-04-08
- Actualización de .gitignore (+26 lines)

### ✅ Commit: 40de2f3 - "feat: Consolidar todas las correcciones y mejoras de Sprint 1"
**Autor:** Sebastian Perez | **Fecha:** 2026-04-08

**Descripción:** Consolidación de todos los cambios y correcciones realizadas durante Sprint 1.

---

### ✅ Commit: dd3caa0 - "docs: Agregar informe de evaluacion de cumplimiento arquitectonico"
**Autor:** Sebastian Perez | **Fecha:** 2026-04-08

**Descripción:** Informe de cumplimiento arquitectónico con puntuación de 98/100.

---

## Fusiones de Ramas (8 de abril de 2026)

### ✅ Commit: 657d752 - "Merge dev3-audit to main"
**Autor:** Sebastian Perez
- Integración de rama dev3-audit (auditoría)

### ✅ Commit: 71968e5 - "Merge dev1-infra-users to main"
**Autor:** Sebastian Perez
- Integración de rama dev1-infra-users (usuarios e infraestructura)

### ✅ Commit: cb32bd5 - "Add dev2: Supplier and Contract Services"
**Autor:** Sebastian Perez

**Servicios añadidos:**
- `contrato-service/`
- `proveedor-service/`

---

## Rama dev3-audit (7 de abril de 2026)

### ✅ Commit: 299aa2b - "validacion de datos mas exacta"
**Autor:** Andres | **Fecha:** 2026-04-07
- Validación mejorada en audit-service

### ✅ Commit: 414397a - "coneccion correcta con eureka"
**Autor:** Andres | **Fecha:** 2026-04-07
- Corrección de conexión con Eureka

### ✅ Commit: ae90b1a - "Solucion de errores"
**Autor:** Andres | **Fecha:** 2026-04-07
- Corrección de errores generales

### ✅ Commit: f217142 - "eliminacion y arreglo"
**Autor:** Lina Xiomara Ladino Fernandez | **Fecha:** 2026-04-07
- Limpieza y correcciones en rama de auditoría

---

## Pull Requests Mergeadas (7 de abril de 2026)

### ✅ PR #6 - dev3-audit
**Autor:** Edgar Sebastian Pérez Díaz | **Fecha:** 2026-04-07
- Merge del servicio de auditoría

### ✅ PR #5 - dev2-providers-contracts
**Autor:** Edgar Sebastian Pérez Díaz | **Fecha:** 2026-04-07
- Merge de servicios de proveedores y contratos

### ✅ PR #4 - dev1-infra-users
**Autor:** Edgar Sebastian Pérez Díaz | **Fecha:** 2026-04-07
- Merge de infraestructura y usuarios

### ✅ PR #3 y #2 - Merges anteriores
**Autor:** Edgar Sebastian Pérez Díaz | **Fecha:** 2026-04-07

---

## Cambios Iniciales (6-7 de abril de 2026)

### ✅ Commit: 74afe57 - "Clean main branch"
**Autor:** Sebastian Perez | **Fecha:** 2026-04-07
- Movimiento de servicios a rama dedicada

### ✅ Commit: 47d1d28 - "dev1"
**Autor:** Lina Xiomara Ladino Fernandez | **Fecha:** 2026-04-07

### ✅ Commit: ab52fdf - "Merge pull request #1"
**Autor:** HolguinnB | **Fecha:** 2026-04-06

### ✅ Commit: 7880128 - "Agrego mi trabajo completo como dev2"
**Autor:** Braya Yesid Holguin Zorro | **Fecha:** 2026-04-03
- Aporte completo de trabajo como desarrollador 2

---

## Sprint 1 - Documentación y Setup Inicial (24 marzo - 3 abril de 2026)

### ✅ Commit: 61c47f0 - "Instrucciones para levantar el microservicio"
**Autor:** Andres | **Fecha:** 2026-04-03
- Documentación de instrucciones de ejecución

### ✅ Commit: cb906b0 - "rama de auditoria"
**Autor:** Andres | **Fecha:** 2026-04-03
- Rama inicial de auditoría

### ✅ Commit: 9db8b65 - "Sprint 1: Delivery summary and project documentation"
**Autor:** Sebastian Perez | **Fecha:** 2026-03-24
- Resumen de entrega de Sprint 1

### ✅ Commit: c504c0d - "Sprint 1: API contracts, database schemas, and technical decisions"
**Autor:** Sebastian Perez | **Fecha:** 2026-03-24

**Documentación añadida:**
- Contratos OpenAPI 3.0
- Esquemas de base de datos (SQL + MongoDB)
- Decisiones técnicas del proyecto

### ✅ Commit: 85f9a1f - "Create users.sql"
**Autor:** Edgar Sebastian Pérez Díaz | **Fecha:** 2026-03-24

### ✅ Commit: 7d78f9a - "Create technology decisions document"
**Autor:** Edgar Sebastian Pérez Díaz | **Fecha:** 2026-03-24

### ✅ Commit: f191305 - "Create technology-decisions.md"
**Autor:** Edgar Sebastian Pérez Díaz | **Fecha:** 2026-03-24

### ✅ Commit: be723ac - "Initial commit"
**Autor:** Edgar Sebastian Pérez Díaz | **Fecha:** 2026-03-24
- Commit inicial del proyecto

---

## Resumen por Desarrollador

### Sebastian Perez
- **Responsabilidades:** Arquitectura, gatekeeping, consolidación de Sprint
- **Cambios principales:** 
  - Consolidación de Sprint 1
  - Evaluación de cumplimiento arquitectónico
  - Fusiones de ramas
  - Setup inicial del proyecto

### Andres
- **Responsabilidades:** Auditoría, servicios de usuario, integración
- **Cambios principales:**
  - Implementación completa de sistema de auditoría
  - Funcionalidades de usuario y edición
  - Integración de servicios

### Braya Yesid Holguin Zorro
- **Responsabilidades:** Servicios de proveedores y contratos
- **Cambios principales:**
  - Máquina de estados de 6 estados para contratos
  - Servicios de proveedores y contratos
  - Colecciones Postman para testing

### Lina Xiomara Ladino Fernandez
- **Responsabilidades:** Infraestructura y usuarios
- **Cambios principales:**
  - Servicio de usuarios
  - Configuración de seguridad
  - Pruebas unitarias
  - API Gateway

### Edgar Sebastian Pérez Díaz
- **Responsabilidades:** Coordinación y gestión
- **Cambios principales:**
  - Setup inicial
  - Gestión de PRs y merges
  - Documentación técnica

---

## Servicios Implementados

### 1. **api-gateway** (Spring Boot)
- Gateway de API centralizado
- Filtros de autenticación JWT
- Enrutamiento de solicitudes

### 2. **usuario-service** (Spring Boot)
- Gestión de usuarios
- Autenticación y autorización
- Integración con auditoría
- Edición de perfiles de usuario

### 3. **eureka-server** (Spring Boot)
- Descubrimiento de servicios
- Registro dinámico de microservicios
- Health checks

### 4. **contrato-service** (Spring Boot)
- Gestión de contratos
- Máquina de estados de 6 estados
- Historial de cambios de estado
- Integración con auditoría

### 5. **proveedor-service** (Spring Boot)
- Gestión de proveedores
- Clientes internos para contratos y auditoría
- Auditoría de cambios
- Validación de datos

### 6. **audit-service** (Python FastAPI)
- Servicio de auditoría centralizado
- Almacenamiento en MongoDB
- Registro de eventos del sistema
- APIs REST para consulta

---

## Tecnologías Principales Utilizadas

### Backend Java
- Spring Boot 3.x
- Maven para build
- Eureka para descubrimiento de servicios
- OpenFeign para comunicación inter-servicios
- JWT para autenticación

### Backend Python
- FastAPI
- SQLAlchemy
- MongoDB para persistencia
- Pydantic para validación

### Herramientas y Documentación
- OpenAPI 3.0 (Swagger)
- Postman Collections
- SQL y MongoDB schemas
- Docker (docker-compose.yml en audit-service)

---

## Próximos Pasos Recomendados

1. **Testing e Integración:** Ejecutar suite completa de pruebas
2. **CI/CD:** Implementar pipeline de integración continua
3. **Documentación:** Actualizar README con instrucciones de despliegue
4. **Monitoreo:** Implementar métricas y logging centralizado
5. **Seguridad:** Auditoría de seguridad y validación de tokens JWT

---

**Documento generado:** 26 de abril de 2026
**Total de cambios rastreados:** 56 commits
**Período cubierto:** 24 de marzo - 18 de abril de 2026
