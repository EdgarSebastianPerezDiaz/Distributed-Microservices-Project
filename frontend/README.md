# Frontend - Aplicación Angular

## 📋 Descripción

Frontend de la aplicación de microservicios. Aplicación Angular que se conecta con el backend de microservicios a través del API Gateway (puerto 8081).

**Estado:** ✅ **INTEGRACIÓN COMPLETADA**

## ✨ Cambios Realizados (May 19, 2026)

### 1. Servicios Implementados/Actualizados
- ✅ **AuthService** - Login, logout, gestión de tokens JWT
- ✅ **UserService** - CRUD de usuarios + nuevo método `getCurrentUser()` y `updateUserStatus()`
- ✅ **ProveedorService** - NUEVO - Conexión a `/api/proveedores` (endpoints correctos)
- ✅ **HttpInterceptor** - Agrega token Bearer + manejo de 401 Unauthorized

### 2. Componentes Actualizados
- ✅ **LoginComponent** - Funcionando con backend
- ✅ **UserListComponent** - Conectado a UserService
- ✅ **SupplierListComponent** - Actualizado a ProveedorService con endpoints `/api/proveedores`

### 3. Configuración
- ✅ **environment.ts** - API URL configurada a `http://localhost:8081`
- ✅ **environment.prod.ts** - NUEVO - Archivo de producción

### 4. Documentación
- ✅ **QUICKSTART.md** - Guía rápida de 5 pasos
- ✅ **INTEGRACION_FRONTEND_BACKEND.md** - Documentación completa
- ✅ **TROUBLESHOOTING.md** - Guía de problemas comunes

## 🚀 INICIO RÁPIDO

```powershell
# 1. Instalar dependencias
cd frontend/frontend-app
npm install

# 2. Iniciar servidor de desarrollo
ng serve --open

# 3. Login con credenciales
# Usuario: admin
# Contraseña: Admin@123

# 4. Navegar por la aplicación
# - http://localhost:4200/login (login)
# - http://localhost:4200/users (listado de usuarios)
# - http://localhost:4200/suppliers (listado de proveedores)
```

Ver más en: [QUICKSTART.md](QUICKSTART.md)

## 📁 Estructura Actualizada

```
frontend/
├── frontend-app/
│   ├── src/
│   │   ├── app/
│   │   │   ├── services/
│   │   │   │   ├── auth.ts              ✅ Completo
│   │   │   │   ├── user.ts              ✅ Actualizado
│   │   │   │   ├── proveedor.ts         ✅ NUEVO
│   │   │   │   └── supplier.ts          (Legacy)
│   │   │   ├── interceptors/
│   │   │   │   └── auth-interceptor.ts  ✅ Mejorado
│   │   │   ├── components/
│   │   │   │   ├── auth/login/          ✅ Funcional
│   │   │   │   ├── users/               ✅ Conectado
│   │   │   │   └── suppliers/           ✅ Actualizado
│   │   │   ├── guards/                  ✅ En lugar
│   │   │   ├── models/                  ✅ Completo
│   │   │   └── app.config.ts            ✅ Interceptor registrado
│   │   └── environments/
│   │       ├── environment.ts           ✅ Configurado
│   │       └── environment.prod.ts      ✅ NUEVO
│   └── package.json
├── QUICKSTART.md                        ✅ NUEVO
├── INTEGRACION_FRONTEND_BACKEND.md      ✅ NUEVO
└── TROUBLESHOOTING.md                   ✅ NUEVO
```

## 🔌 Endpoints Conectados

### Autenticación (usuario-service → gateway:8081)
| Método | Ruta | Status |
|--------|------|--------|
| POST | `/api/auth/login` | ✅ Conectado |
| GET | `/api/auth/me` | ✅ Implementado |
| GET | `/api/auth/users` | ✅ Conectado |
| POST | `/api/auth/register` | ✅ Conectado |
| PATCH | `/api/users/{id}/status` | ✅ Implementado |

### Proveedores (proveedor-service → gateway:8081)
| Método | Ruta | Status |
|--------|------|--------|
| GET | `/api/proveedores` | ✅ Conectado |
| GET | `/api/proveedores/{id}` | ✅ Conectado |
| POST | `/api/proveedores` | ✅ Conectado |
| PUT | `/api/proveedores/{id}` | ✅ Conectado |
| PATCH | `/api/proveedores/{id}/estado` | ✅ Conectado |
| DELETE | `/api/proveedores/{id}` | ✅ Conectado |

## 🔐 Autenticación

**Tipo:** JWT Legacy HS512 (24 horas)

**Flow:**
1. Usuario ingresa credenciales en `/login`
2. Frontend POST `/api/auth/login` → recibe JWT
3. JWT guardado en localStorage (clave: `token`)
4. HttpInterceptor agrega `Authorization: Bearer <token>` en todas las peticiones
5. Backend valida token en cada request protegido
6. Si expira o es inválido → 401 → Interceptor hace logout y redirige a login

## 🛠️ Requisitos Previos

- **Node.js** 18+ ([descargar](https://nodejs.org))
- **Angular CLI** 17+ (`npm install -g @angular/cli`)
- **Backend corriendo:**
  - API Gateway: `http://localhost:8081`
  - usuario-service: `http://localhost:8084`
  - proveedor-service: `http://localhost:8082`

## 📦 Dependencias Principales

```json
{
  "@angular/core": "^17.0.0",
  "@angular/material": "^17.0.0",
  "@angular/common/http": "^17.0.0",
  "rxjs": "^7.8.0"
}
```

## 🧪 Testing

Abre DevTools (F12) y verifica:

**Network Tab:**
```
✅ POST /api/auth/login              → 200
✅ GET /api/auth/users               → 200
✅ GET /api/proveedores              → 200
✅ Authorization: Bearer <token>     → En headers
```

**Application Tab:**
```
✅ localStorage.token                → JWT (eyJ...)
✅ localStorage.user                 → {id, username, role, ...}
```

## 📚 Documentación

- **[QUICKSTART.md](QUICKSTART.md)** - Guía rápida (5 minutos)
- **[INTEGRACION_FRONTEND_BACKEND.md](INTEGRACION_FRONTEND_BACKEND.md)** - Documentación completa
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Solución de problemas

## 🐛 Troubleshooting

**❌ "No se pude conectar al servidor"**
- Verifica: `curl http://localhost:8081/actuator/health`

**❌ "Credenciales inválidas" en login**
- Usuario: `admin`, Contraseña: `Admin@123`

**❌ "Cannot read property 'content'"**
- Ver: [TROUBLESHOOTING.md](TROUBLESHOOTING.md#problema-listar-proveedores-muestra-error)

## 📞 Servicio al Cliente

1. **Documentación:** Ver archivos `.md` en esta carpeta
2. **Debugging:** DevTools (F12) → Network/Console
3. **Logs:** Angular dev console
4. **Backend logs:** Revisa terminal de cada microservicio

## ✅ Checklist Pre-Producción

- [ ] Backend corriendo en todos los puertos (8081, 8084, 8082)
- [ ] `npm install` ejecutado
- [ ] Login funciona con credenciales admin
- [ ] Listar usuarios funciona
- [ ] Listar proveedores funciona
- [ ] DevTools Network muestra authorization headers
- [ ] No hay errores en console
- [ ] Token se guarda en localStorage

## 📝 Notas Importantes

1. **API Gateway:** Todos los requests van a `http://localhost:8081`
2. **CORS:** Backend debe tener CORS habilitado
3. **Token HS512:** Válido por 24 horas
4. **Roles:** ADMINISTRADOR, FUNCIONARIO, AUDITOR
5. **ProveedorService:** Usa `/api/proveedores` (NO `/api/suppliers`)

## 🔄 Próximos Pasos

- [ ] Testear completo end-to-end
- [ ] Ajustar estructuras de datos si backend retorna diferente
- [ ] Agregar validaciones adicionales
- [ ] Implementar caché si es necesario
- [ ] Deploy a producción

---

**Última actualización:** May 19, 2026  
**Versión:** 1.0  
**Estado:** ✅ Listo para testing
│   │   │   │   ├── user.model.ts
│   │   │   │   └── response.model.ts
│   │   │   ├── app.routes.ts
│   │   │   └── app.component.ts
│   │   ├── environments/
│   │   │   ├── environment.ts
│   │   │   └── environment.prod.ts
│   │   ├── main.ts
│   │   └── index.html
│   ├── angular.json
│   ├── package.json
│   └── tsconfig.json
└── README.md
```

## 🎯 Componentes a Crear

### 1. **LoginComponent**
- Formulario con username/password
- POST a `/api/auth/login` via API Gateway (8081)
- Guarda JWT token en localStorage
- Redirecciona a /users-list si exitoso

### 2. **RegisterComponent** (Solo ADMIN)
- Formulario: username, email, password, fullName, role
- POST a `/api/auth/register`
- Requiere autenticación como ADMIN
- Muestra mensaje de éxito/error

### 3. **UserListComponent**
- GET `/api/auth/users` (requiere ADMIN)
- Tabla con usuarios (username, email, role, active)
- Botones: Edit, Deactivate
- Paginación (opcional)

### 4. **UserProfileComponent**
- GET `/api/auth/me` (protegido con JWT)
- Muestra datos del usuario actual
- Opción para cambiar contraseña (si existe endpoint)

### 5. **NavbarComponent**
- Links: Home, Users (si ADMIN), Profile, Logout
- Muestra username del usuario logueado
- Logout limpia token y redirecciona a /login

## 🔐 Servicios a Crear

### AuthService
```typescript
- login(username, password): Observable<{ token, user }>
- register(userData): Observable<User>
- logout(): void
- isAuthenticated(): boolean
- getCurrentUser(): User | null
- getToken(): string
```

### UserService
```typescript
- getUsers(): Observable<User[]>
- getUser(id): Observable<User>
- updateUser(id, data): Observable<User>
- deactivateUser(id): Observable<User>
- getProfile(): Observable<User>
```

### ApiInterceptor
- Agrega `Authorization: Bearer {token}` a todos los requests
- Maneja 401 Unauthorized (logout automático)
- Maneja errores globales

## 🛡️ Guards y Interceptors

### AuthGuard
- Protege rutas que requieren autenticación
- Redirecciona a /login si no está autenticado

## 📦 Dependencias Principal

- `@angular/core` (v18.x)
- `@angular/forms` (reactive forms)
- `@angular/common/http` (HTTP client)
- `@angular/router` (routing)
- Bootstrap 5 (opcional para estilos)

## 🚀 Pasos de Generación (PASO 3)

```bash
# 1. Crear proyecto Angular
ng new frontend-dev1 --routing --style=css --package-manager npm

# 2. Navegar a proyecto
cd frontend-dev1

# 3. Generar componentes
ng generate component components/login
ng generate component components/register
ng generate component components/user-list
ng generate component components/user-profile
ng generate component components/navbar

# 4. Generar servicios
ng generate service services/auth
ng generate service services/user
ng generate service services/api.interceptor

# 5. Generar guard
ng generate guard guards/auth

# 6. Configurar environment
# Editar src/environments/environment.ts:
# const environment = { apiUrl: 'http://localhost:8081' };

# 7. Iniciar servidor
ng serve --port 4200
```

## 🔗 Integración Backend-Frontend

**API Gateway:** `http://localhost:8081`

**Rutas del API:**
- `POST /api/auth/login` → Obtener JWT token
- `POST /api/auth/register` → Crear usuario (ADMIN only)
- `GET /api/auth/users` → Listar usuarios (ADMIN only)
- `GET /api/auth/me` → Datos del usuario actual
- `PUT /api/auth/users/{id}` → Actualizar usuario (ADMIN only)
- `PATCH /api/auth/users/{id}/status` → Desactivar usuario (ADMIN only)

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

## 📝 Configuración del Environment

### environment.ts (Desarrollo)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081',
  jwtTokenKey: 'auth_token'
};
```

### environment.prod.ts (Producción)
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.production.domain.com',
  jwtTokenKey: 'auth_token'
};
```

## 📋 Checklist Generación Frontend

- [ ] `ng new frontend-dev1` ejecutado
- [ ] Componentes generados (5 componentes)
- [ ] Servicios generados (2 servicios)
- [ ] Guard creado (auth.guard)
- [ ] Modelos TypeScript definidos
- [ ] Rutas configuradas en app.routes.ts
- [ ] HTTP Interceptor implementado
- [ ] Environment configurado con apiUrl
- [ ] Estilos CSS básicos
- [ ] Login funcional conectado a backend
- [ ] Tabla de usuarios funcional
- [ ] JWT token guardado en localStorage
- [ ] Auto-logout en 401

## 🧪 Pruebas Manuales

```bash
# 1. Iniciar backend (ya está running en PASO 2)
# Puerto 8761 (Eureka), 8081 (Gateway), 8084 (Usuario)

# 2. Iniciar frontend
cd frontend/frontend-dev1
npm install  # si es primera vez
ng serve --port 4200

# 3. Abrir navegador
http://localhost:4200

# 4. Probar flows
- Login con admin/admin123
- Ver listado de usuarios (GET /api/auth/users)
- Ver perfil actual (GET /api/auth/me)
- Crear usuario (si es ADMIN)
- Logout
```

## 📚 Documentación Adicional

- Backend API: `/backend/docs/api-contracts/openapi-users.yaml`
- Security: JWT HS512, Bearer token
- Database: PostgreSQL db_usuarios
- Estatus Actual: [Ver VERIFICACION_BACKEND_DEV1.md](../VERIFICACION_BACKEND_DEV1.md)

---

**Estado:** Estructura preparada, pronta para generación de Angular  
**Integración:** Backend verificado ✅ en Puerto 8084 (Usuario Service)  
**Próximo Paso:** PASO 3 - Generar proyecto Angular
