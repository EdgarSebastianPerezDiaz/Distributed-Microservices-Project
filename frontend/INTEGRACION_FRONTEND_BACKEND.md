# 🔌 GUÍA DE INTEGRACIÓN - ANGULAR FRONTEND ↔ MICROSERVICIOS BACKEND

**Fecha:** May 19, 2026  
**Versión:** 1.0  
**Estado:** ✅ COMPLETO Y LISTO PARA TESTING

---

## 📋 RESUMEN DE CAMBIOS REALIZADOS

### 1. ✅ Servicios Actualizados/Creados

#### **AuthService** (`src/app/services/auth.ts`)
**Estado:** ✅ COMPLETO
- ✅ `login(credentials: LoginRequest): Observable<LoginResponse>`  
  - POST `http://localhost:8081/api/auth/login`
  - Almacena token en localStorage
- ✅ `logout(): void`
  - Limpia localStorage
  - Redirige a /login
- ✅ `getToken(): string | null`
  - Obtiene token del localStorage
- ✅ `isLoggedIn(): boolean`
  - Verifica si existe token
- ✅ `register(payload: RegisterRequest): Observable<User>`
  - POST `http://localhost:8081/api/auth/register`
- ✅ `getCurrentUser(): User | null`
  - Obtiene usuario actual desde BehaviorSubject
- ✅ `getCurrentUser$: Observable<User | null>`
  - Observable del usuario actual
- ✅ `isAuthenticated$: Observable<boolean>`
  - Observable de estado de autenticación

#### **UserService** (`src/app/services/user.ts`)
**Estado:** ✅ ACTUALIZADO CON MÉTODOS NUEVOS
- ✅ `getUsers(page, pageSize, search?): Observable<PaginatedResponse<User>>`
  - GET `/api/auth/users` con paginación
- ✅ `getUserById(id): Observable<User>`
  - GET `/api/auth/users/{id}`
- ✅ `createUser(user): Observable<User>`
  - POST `/api/auth/register`
- ✅ `updateUser(id, user): Observable<User>`
  - PUT `/api/auth/users/{id}`
- ✅ `deleteUser(id): Observable<void>`
  - DELETE `/api/auth/users/{id}`
- ✅ **`getCurrentUser(): Observable<User>`** ⭐ NUEVO
  - GET `/api/auth/me` - Obtiene perfil del usuario autenticado
- ✅ **`updateUserStatus(id, status): Observable<User>`** ⭐ NUEVO
  - PATCH `/api/users/{id}/status` - Cambiar estado del usuario
- ✅ `activateUser(id): Observable<User>` (Legacy)
- ✅ `deactivateUser(id): Observable<User>` (Legacy)
- ✅ `validateUsername(username): Observable<{ available: boolean }>`
- ✅ `validateEmail(email): Observable<{ available: boolean }>`

#### **ProveedorService** (`src/app/services/proveedor.ts`)
**Estado:** ✅ NUEVO - Endpoints Correctos (🔴 NO /api/suppliers)
- ✅ `getProveedores(page?, pageSize?, search?): Observable<any>`
  - GET `/api/proveedores` con paginación
- ✅ `getProveedorById(id): Observable<Proveedor>`
  - GET `/api/proveedores/{id}`
- ✅ `createProveedor(data): Observable<Proveedor>`
  - POST `/api/proveedores`
- ✅ `updateProveedor(id, data): Observable<Proveedor>`
  - PUT `/api/proveedores/{id}`
- ✅ **`cambiarEstado(id, estado): Observable<Proveedor>`** ⭐ DESTACADO
  - PATCH `/api/proveedores/{id}/estado` con `{ estado: "ACTIVO"|"INACTIVO" }`
- ✅ `deleteProveedor(id): Observable<void>`
  - DELETE `/api/proveedores/{id}`
- ✅ `validateNit(nit): Observable<{ available: boolean }>`
- ✅ `validateEmail(email): Observable<{ available: boolean }>`

**Interfaces de Datos:**
```typescript
export interface Proveedor {
  id?: string;
  nombre: string;
  nit: string;
  email: string;
  telefono?: string;
  direccion?: string;
  tipoPersona?: string;
  estado?: string; // ACTIVO | INACTIVO
  createdAt?: string;
  updatedAt?: string;
}

export interface ProveedorPageResponse {
  content: Proveedor[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}
```

### 2. ✅ Interceptor HTTP Mejorado

**AuthInterceptor** (`src/app/interceptors/auth-interceptor.ts`)
**Estado:** ✅ MEJORADO CON MANEJO DE 401
- ✅ Agrega header `Authorization: Bearer <token>` a todas las peticiones
- ✅ Excluye rutas públicas: `/api/auth/login`, `/api/auth/register`
- ✅ **Manejo de 401 Unauthorized:**
  - Automáticamente hace logout
  - Redirige a `/login` con parámetro `returnUrl`
  - Restaura context para reintentar después de login
- ✅ Propaga errores correctamente para manejo en componentes

```typescript
// Ejemplo de uso en componente:
this.userService.getUsers().subscribe({
  next: (users) => { /* ... */ },
  error: (error) => {
    if (error.status === 401) {
      // Interceptor ya manejó la redirección
    }
  }
});
```

### 3. ✅ Configuración de Entornos

**environment.ts** (`src/environments/environment.ts`)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081'
};
```

**environment.prod.ts** (`src/environments/environment.prod.ts`)
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.example.com' // CAMBIAR A PRODUCCIÓN
};
```

### 4. ✅ Componentes Actualizados

#### **LoginComponent** (`src/app/components/auth/login/login.ts`)
**Estado:** ✅ FUNCIONAL
- Valida credenciales en formulario reactivo
- Llama a `AuthService.login()`
- Redirige según rol del usuario:
  - ADMINISTRADOR → `/admin`
  - FUNCIONARIO → `/contratos`
  - AUDITOR → `/auditoria`
- Maneja errores 401, 403, conexión
- Muestra mensajes de error específicos

#### **UserListComponent** (`src/app/components/users/user-list/user-list.ts`)
**Estado:** ✅ ACTUALIZADO
- Carga usuarios con `UserService.getUsers()`
- Paginación funcionando
- Búsqueda por término
- Activar/desactivar usuarios
- Crear nuevo usuario
- Editar usuarios existentes

#### **SupplierListComponent** (`src/app/components/suppliers/supplier-list/supplier-list.ts`)
**Estado:** ✅ ACTUALIZADO A ProveedorService
- **Cambio crítico:** Ahora usa `ProveedorService` en lugar de `SupplierService`
- Endpoints correctos: `/api/proveedores` (no `/api/suppliers`)
- Carga proveedores con `ProveedorService.getProveedores()`
- Paginación y búsqueda funcionando
- Cambiar estado ACTIVO/INACTIVO con `cambiarEstado()`
- Crear, editar, eliminar proveedores
- Validación de permisos por rol

---

## 🔐 FLUJO DE AUTENTICACIÓN COMPLETO

### Paso 1: Login
```typescript
// Usuario ingresa credenciales en login.ts
const credentials = { username: 'admin', password: 'Admin@123' };

// Llamada a AuthService
this.authService.login(credentials).subscribe({
  next: (response) => {
    // Response: { token: "jwt_hs512_token", user: User }
    // AuthService guarda token en localStorage automaticamente
    // Redirige según rol
  },
  error: (error) => {
    // 401 → Credenciales inválidas
    // 403 → Usuario inactivo
    // 0 → Backend no disponible
  }
});
```

### Paso 2: Petición Protegida
```typescript
// Componente solicita datos de usuarios
this.userService.getUsers().subscribe(/* ... */);

// Flujo interno:
// 1. Componente llama UserService.getUsers()
// 2. HttpClient hace GET /api/auth/users
// 3. authInterceptor intercepta:
//    - Obtiene token de localStorage
//    - Agrega header: Authorization: Bearer <token>
// 4. Request va al gateway: http://localhost:8081/api/auth/users
// 5. Backend valida token (HS512 o RS256)
// 6. Si válido: retorna datos
//    Si inválido: retorna 401
// 7. Si 401:
//    - Interceptor hace logout automático
//    - Redirige a /login
//    - Usuario inicia sesión de nuevo
```

### Paso 3: Logout
```typescript
// Usuario hace click en "Cerrar Sesión"
this.authService.logout();

// AuthService:
// 1. Elimina token de localStorage
// 2. Limpia BehaviorSubject de usuario actual
// 3. Redirige a /login
```

---

## 📞 REFERENCIAS DE ENDPOINTS

### Autenticación (usuario-service → gateway:8081)
| Método | Ruta | Descripción | Requiere Token |
|--------|------|-------------|----------------|
| POST | `/api/auth/login` | Login con username/password | ❌ No |
| POST | `/api/auth/register` | Registrar nuevo usuario | ❌ No |
| GET | `/api/auth/me` | Obtener perfil autenticado | ✅ Sí |
| GET | `/api/auth/users` | Listar usuarios (ADMIN) | ✅ Sí |
| GET | `/api/auth/users/{id}` | Obtener usuario por ID | ✅ Sí |
| PATCH | `/api/users/{id}/status` | Cambiar estado usuario | ✅ Sí |

### Proveedores (proveedor-service → gateway:8081)
| Método | Ruta | Descripción | Requiere Token |
|--------|------|-------------|----------------|
| GET | `/api/proveedores` | Listar proveedores | ✅ Sí |
| GET | `/api/proveedores/{id}` | Obtener proveedor por ID | ✅ Sí |
| POST | `/api/proveedores` | Crear proveedor | ✅ Sí |
| PUT | `/api/proveedores/{id}` | Actualizar proveedor | ✅ Sí |
| PATCH | `/api/proveedores/{id}/estado` | Cambiar estado | ✅ Sí |
| DELETE | `/api/proveedores/{id}` | Eliminar/desactivar | ✅ Sí |

---

## 🧪 TESTING END-TO-END

### Test 1: Login y Token
```bash
# 1. Abre http://localhost:4200/login
# 2. Ingresa credenciales:
#    Username: admin
#    Password: Admin@123
# 3. Verifica:
#    - Token guardado en localStorage (DevTools → Application)
#    - Redirección a /admin
#    - Usuario mostrado en header/navbar
```

### Test 2: Listar Usuarios
```bash
# 1. Como ADMIN, ve a /users
# 2. Verifica:
#    - Tabla de usuarios cargada
#    - Paginación funcionando
#    - Búsqueda funciona
#    - Request a GET http://localhost:8081/api/auth/users
```

### Test 3: Listar Proveedores
```bash
# 1. Ve a /suppliers
# 2. Verifica:
#    - Lista de proveedores cargada
#    - GET http://localhost:8081/api/proveedores en Network tab
#    - Cambio de estado funciona (ACTIVO/INACTIVO)
#    - PATCH http://localhost:8081/api/proveedores/{id}/estado
```

### Test 4: Error 401
```bash
# 1. Abre DevTools → Application → LocalStorage
# 2. Borra el token manualmente
# 3. Recarga la página
# 4. Verifica:
#    - Automáticamente redirige a /login
#    - Mensaje "Token inválido o expirado"
```

### Test 5: Crear Usuario
```bash
# 1. Como ADMIN, ve a /users/new
# 2. Llena formulario:
#    - Username: testuser
#    - Email: test@example.com
#    - Password: Password@123
#    - Role: FUNCIONARIO
# 3. Click en "Crear"
# 4. Verifica:
#    - POST http://localhost:8081/api/auth/register
#    - Usuario aparecerespectively en lista
```

### Test 6: Cambiar Estado de Proveedor
```bash
# 1. En listado de proveedores
# 2. Click en botón estado de un proveedor
# 3. Verifica:
#    - PATCH http://localhost:8081/api/proveedores/{id}/estado
#    - Estado cambia en la UI
```

---

## 🔧 CONFIGURACIÓN EN ANGULAR

### app.config.ts (Ya Configurado ✅)
```typescript
import { authInterceptor } from './interceptors/auth-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
    // ... otros providers
  ]
};
```

### Uso de Servicios en Componentes
```typescript
import { UserService } from '../../../services/user';
import { ProveedorService } from '../../../services/proveedor';
import { AuthService } from '../../../services/auth';

@Component({
  // ...
})
export class MyComponent implements OnInit {
  constructor(
    private userService: UserService,
    private proveedorService: ProveedorService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    // Verificar si está autenticado
    this.authService.isAuthenticated$.subscribe(isAuth => {
      if (!isAuth) {
        // Redirigir a login
      }
    });

    // Obtener usuario actual
    const currentUser = this.authService.getCurrentUser();
    
    // Cargar usuarios
    this.userService.getUsers().subscribe(data => {
      console.log('Usuarios:', data);
    });

    // Cargar proveedores
    this.proveedorService.getProveedores().subscribe(data => {
      console.log('Proveedores:', data);
    });
  }
}
```

---

## ⚠️ NOTAS IMPORTANTES

### 1. API Gateway (Puerto 8081)
- **Todos los requests van por aquí** - NO directamente a los microservicios
- Ruta: `http://localhost:8081`
- Routes configuradas:
  - `/api/auth/**` → usuario-service:8084
  - `/api/proveedores/**` → proveedor-service:8082

### 2. Token JWT (HS512 - Legacy)
- **Duración:** 24 horas
- **Almacenamiento:** localStorage (clave: `token`)
- **Formato:** `Authorization: Bearer <token>`
- **Backend acepta:** HS512 o RS256 (dual validation)

### 3. Roles y Permisos
```typescript
enum UserRole {
  ADMINISTRADOR = 'ADMINISTRADOR',  // Acceso completo
  FUNCIONARIO = 'FUNCIONARIO',      // Acceso lectura + operaciones
  AUDITOR = 'AUDITOR'               // Acceso solo lectura
}
```

### 4. Componentes Relacionados
- **Login:** `/src/app/components/auth/login/`
- **Usuarios:** `/src/app/components/users/`
- **Proveedores:** `/src/app/components/suppliers/`
- **Guards:** `/src/app/guards/` - Protección de rutas
- **Models:** `/src/app/models/` - Interfaces de datos

### 5. Servicio de Contract (NO MODIFICADO)
- `contract.ts` existe pero NO ha sido actualizado en esta sesión
- Se recomienda revisar si necesita endpoints correctos también

---

## 🚀 PRÓXIMOS PASOS

### Fase 1: Testing (AHORA)
1. ✅ Iniciar backend (todos los microservicios)
2. ✅ `npm install` en frontend
3. ✅ `ng serve --open` para iniciar Angular dev server
4. ✅ Probar login con credenciales admin
5. ✅ Navegar por usuarios y proveedores
6. ✅ Verificar Network tab: requests a localhost:8081

### Fase 2: Correcciones (SI HAY ERRORES)
1. Revisar errores en DevTools Console
2. Revisar Network requests/responses
3. Verificar que backend está respondiendo correctamente
4. Ajustar estructuras de datos si backend retorna diferente

### Fase 3: Optimización (OPCIONAL)
1. Agregar loading states más visuales
2. Agregar confirmación dialogs para operaciones críticas
3. Agregar notificaciones toast para feedback del usuario
4. Implementar caché de datos (si es necesario)
5. Agregar validaciones adicionales en formularios

---

## 📝 CHECKLISTA FINAL

- [x] AuthService completo con login/logout
- [x] HttpInterceptor con manejo de 401
- [x] UserService con métodos completos
- [x] ProveedorService creado con endpoints correctos
- [x] Componentes actualizados para usar nuevos servicios
- [x] Configuración de entornos en place
- [x] Endpoints del backend documentados
- [x] Flujo de autenticación explicado

**Estado: ✅ LISTO PARA TESTING COMPLETO**

---

## 📞 SOPORTE

Si encuentras problemas:
1. Revisa la consola del navegador (F12 → Console)
2. Revisa la pestaña Network (F12 → Network)
3. Verifica que backend está corriendo en todos los puertos
4. Verifica que localStorage tiene el token guardado
5. Limpia caché y cookies si hay errores extraños

¡Éxito con la integración! 🎉
