# Frontend - Dev1

## 📋 Descripción

Carpeta dedicada al frontend del proyecto **Dev1** (Desarrollador 1). Aquí se generará la aplicación Angular que se integra con el backend de microservicios.

## 🏗️ Estructura (A Generar)

```
frontend/
├── frontend-dev1/                 # Proyecto Angular (ng new --routing --style=css)
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/
│   │   │   │   ├── login/
│   │   │   │   │   ├── login.component.ts
│   │   │   │   │   ├── login.component.html
│   │   │   │   │   └── login.component.css
│   │   │   │   ├── register/
│   │   │   │   ├── user-list/
│   │   │   │   ├── user-profile/
│   │   │   │   └── navbar/
│   │   │   ├── services/
│   │   │   │   ├── auth.service.ts
│   │   │   │   ├── user.service.ts
│   │   │   │   └── api.interceptor.ts
│   │   │   ├── guards/
│   │   │   │   └── auth.guard.ts
│   │   │   ├── models/
│   │   │   │   ├── auth.model.ts
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
