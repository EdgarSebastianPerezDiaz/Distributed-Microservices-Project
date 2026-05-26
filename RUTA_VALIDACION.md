# Validación de Rutas Frontend ↔ Backend

## ✅ USUARIOS (usuario-service)

### Login
- **Frontend**: `POST /api/auth/login` → auth.service.ts
- **Backend**: `POST /api/auth/login` → AuthController
- **Status**: ✅ ALINEADO

### Listar Usuarios
- **Frontend**: `GET /api/auth/users` (con paginación) → user.service.ts
- **Backend**: `GET /api/auth/users` (PreAuthorize ADMINISTRADOR) → AuthController
- **Status**: ✅ ALINEADO

### Obtener Usuario por ID
- **Frontend**: `GET /api/auth/users/{id}` → user.service.ts
- **Backend**: `GET /api/auth/users/{id}` (PreAuthorize ADMINISTRADOR) → AuthController
- **Status**: ✅ ALINEADO

### Crear Usuario (Registrar)
- **Frontend**: `POST /api/auth/register` → user.service.ts (createUser)
- **Backend**: `POST /api/auth/register` → AuthController (público)
- **Status**: ✅ ALINEADO

### Actualizar Usuario
- **Frontend**: `PUT /api/auth/users/{id}` (body: {email, fullName, role}) → user.service.ts
- **Backend**: `PUT /api/users/{id}` (PreAuthorize ADMINISTRADOR) → UserController
- **Status**: ⚠️ RUTAS DIFERENTES - Frontend usa `/api/auth/users/{id}`, Backend usa `/api/users/{id}`
- **Fix**: Cambiar frontend a `/api/users/{id}` O agregar ruta en AuthController

### Cambiar Estado Usuario
- **Frontend**: `PATCH /api/auth/users/{id}/estado` (body: {estado: true/false}) → user.service.ts
- **Backend**: `PATCH /api/auth/users/{id}/estado` (PreAuthorize ADMINISTRADOR) → AuthController
- **Status**: ✅ ALINEADO

### Cambiar Estado Usuario (UserController)
- **Backend**: `PATCH /api/users/{id}/estado` (PreAuthorize ADMINISTRADOR) → UserController
- **Status**: ✅ Alternativa disponible

### Eliminar Usuario
- **Frontend**: `DELETE /api/auth/users/{id}` → user.service.ts
- **Backend**: `DELETE /api/auth/users/{id}` (PreAuthorize ADMINISTRADOR) → AuthController
- **Status**: ✅ ALINEADO

### Validar Username
- **Frontend**: `GET /api/auth/users/validate/username` → user.service.ts
- **Backend**: No encontrado en controladores revisados
- **Status**: ⚠️ ENDPOINT FALTANTE EN BACKEND

### Validar Email
- **Frontend**: `GET /api/auth/users/validate/email` → user.service.ts
- **Backend**: No encontrado en controladores revisados
- **Status**: ⚠️ ENDPOINT FALTANTE EN BACKEND

---

## ✅ PROVEEDORES (proveedor-service)

### Listar Proveedores
- **Frontend**: `GET /api/suppliers` (con paginación) → supplier.service.ts
- **Backend**: `GET /api/suppliers` (PreAuthorize ADMINISTRADOR, FUNCIONARIO, AUDITOR) → SupplierController
- **Status**: ✅ ALINEADO

### Obtener Proveedor por ID
- **Frontend**: `GET /api/suppliers/{id}` → supplier.service.ts
- **Backend**: `GET /api/suppliers/{id}` (PreAuthorize ADMINISTRADOR, FUNCIONARIO, AUDITOR) → SupplierController
- **Status**: ✅ ALINEADO

### Obtener Proveedor por NIT
- **Frontend**: No implementado
- **Backend**: `GET /api/suppliers/nit/{nit}` (PreAuthorize ADMINISTRADOR, FUNCIONARIO, AUDITOR) → SupplierController
- **Status**: ✅ Disponible en backend

### Crear Proveedor
- **Frontend**: `POST /api/suppliers` (body: Supplier) → supplier.service.ts
- **Backend**: `POST /api/suppliers` (PreAuthorize ADMINISTRADOR) → SupplierController
- **Status**: ✅ ALINEADO

### Actualizar Proveedor
- **Frontend**: `PUT /api/suppliers/{id}` (body: Supplier) → supplier.service.ts
- **Backend**: `PUT /api/suppliers/{id}` (PreAuthorize ADMINISTRADOR) → SupplierController
- **Status**: ✅ ALINEADO

### Cambiar Estado Proveedor
- **Frontend**: `PATCH /api/suppliers/{id}/estado` (body: {estado: 'HABILITADO'|'INHABILITADO'}) → supplier.service.ts (changeStatus)
- **Backend**: `PATCH /api/suppliers/{id}/estado` (RequestBody SupplierStatusChangeRequest) → SupplierController
- **Status**: ✅ ALINEADO (ACTUALIZADO)

### Eliminar Proveedor
- **Frontend**: `DELETE /api/suppliers/{id}` → supplier.service.ts
- **Backend**: No encontrado en SupplierController
- **Status**: ⚠️ ENDPOINT FALTANTE EN BACKEND

### Validar NIT
- **Frontend**: `GET /api/suppliers/validate/nit` → supplier.service.ts
- **Backend**: No encontrado en SupplierController
- **Status**: ⚠️ ENDPOINT FALTANTE EN BACKEND

### Validar Email
- **Frontend**: `GET /api/suppliers/validate/email` → supplier.service.ts
- **Backend**: No encontrado en SupplierController
- **Status**: ⚠️ ENDPOINT FALTANTE EN BACKEND

---

## Problemas Identificados

### CRÍTICOS (rompen funcionalidad):
1. ❌ Ruta conflictiva: Frontend usa `PUT /api/auth/users/{id}` pero backend usa `PUT /api/users/{id}`
   - **Recomendación**: Usar `PUT /api/users/{id}` en backend y frontend debe apuntar a UserController (`/api/users/`)

### SECUNDARIOS (validación):
1. ⚠️ Endpoints de validación faltantes en usuario-service (validate/username, validate/email)
2. ⚠️ Endpoints de validación faltantes en proveedor-service (validate/nit, validate/email)
3. ⚠️ DELETE proveedor no implementado en SupplierController

---

## Cambios Realizados

### Backend
1. ✅ AuthController: `/users/{id}/status` → `/users/{id}/estado`
2. ✅ UserController: `/users/{id}/status` → `/users/{id}/estado`
3. ✅ SupplierController: `/suppliers/{id}/status` → `/suppliers/{id}/estado` (con RequestBody)
4. ✅ SupplierStatusChangeRequest DTO creado
5. ✅ SupplierStatus enum: ACTIVO/INACTIVO → HABILITADO/INHABILITADO
6. ✅ SupplierService actualizado para usar HABILITADO/INHABILITADO
7. ✅ SupplierMapper actualizado

### Frontend
- ✅ user.service.ts: ya usa `/api/auth/users/{id}/estado` correctamente
- ✅ supplier.service.ts: ya usa `/api/suppliers/{id}/estado` correctamente
- ✅ Modelos actualizados (SupplierStatus enum)

---

## Próximos Pasos Recomendados

1. Resolver conflicto de ruta para UPDATE usuario (decidir entre `/api/auth/users/{id}` o `/api/users/{id}`)
2. Implementar endpoints de validación faltantes
3. Verificar que API Gateway mapea correctamente todas las rutas
4. Ejecutar tests E2E para validar completitud
