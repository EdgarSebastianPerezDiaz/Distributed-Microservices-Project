# ⚡ QUICK START - FRONTEND ANGULAR

**Tiempo de setup:** 5 minutos  
**Prerequisitos:** Backend corriendo en puertos 8081, 8084, 8082

---

## 🎬 EN 5 PASOS

### Paso 1: Instalar dependencias (si no lo hizo)
```powershell
cd C:\Users\DELL\Downloads\Distribuidos\Proyecto-distribuidos\Distributed-Microservices-Project\frontend\frontend-app
npm install
```
⏱️ **Espera:** 3-5 minutos (primera vez)

### Paso 2: Iniciar servidor de desarrollo
```powershell
ng serve --open
```
✅ Abrirá automáticamente `http://localhost:4200`

### Paso 3: Login
- **URL:** `http://localhost:4200/login`
- **Usuario:** `admin`
- **Contraseña:** `Admin@123`
- **Click:** Botón "INICIAR SESIÓN"

### Paso 4: Dashboard
- ✅ Redirigido a `/admin` (si eres ADMINISTRADOR)
- ✅ Token guardado en localStorage
- ✅ Headers del navegador muestran usuario logueado

### Paso 5: Probar funcionalidades
- **Usuarios:** http://localhost:4200/users
  - Ver lista de usuarios
  - Crear/editar/cambiar estado
  
- **Proveedores:** http://localhost:4200/suppliers
  - Ver lista de proveedores
  - Cambiar estado ACTIVO/INACTIVO
  - Crear/editar/eliminar

---

## ✅ CHECKLIST DE VERIFICACIÓN

Abre DevTools (F12) y verifica en **Network tab**:

```
✅ POST /api/auth/login                  → 200 (login)
✅ GET /api/auth/users                   → 200 (usuarios)
✅ GET /api/proveedores                  → 200 (proveedores)
✅ PATCH /api/proveedores/{id}/estado    → 200 (cambio estado)
✅ Authorization: Bearer <token>         → En header de cada request
```

**Application tab:**
```
✅ LocalStorage → token: "eyJ..."
✅ LocalStorage → user: "{...}"
```

---

## 🔌 ENDPOINTS CONECTADOS

| Servicio | Endpoint | Status |
|----------|----------|--------|
| **Autenticación** | POST `/api/auth/login` | ✅ Conectado |
| **Perfil** | GET `/api/auth/me` | ✅ Implementado |
| **Usuarios** | GET `/api/auth/users` | ✅ Conectado |
| **Crear Usuario** | POST `/api/auth/register` | ✅ Conectado |
| **Cambiar Estado Usuario** | PATCH `/api/users/{id}/status` | ✅ Implementado |
| **Proveedores** | GET `/api/proveedores` | ✅ Conectado |
| **Proveedor por ID** | GET `/api/proveedores/{id}` | ✅ Conectado |
| **Crear Proveedor** | POST `/api/proveedores` | ✅ Conectado |
| **Actualizar Proveedor** | PUT `/api/proveedores/{id}` | ✅ Conectado |
| **Cambiar Estado Proveedor** | PATCH `/api/proveedores/{id}/estado` | ✅ Conectado |
| **Eliminar Proveedor** | DELETE `/api/proveedores/{id}` | ✅ Conectado |

---

## 🐛 SI ALGO NO FUNCIONA

**99% de problemas se deben a:**

1. **Backend no está corriendo:**
   ```powershell
   # Verifica en otra terminal
   curl http://localhost:8081/actuator/health
   # Debe retornar: {"status":"UP"}
   ```

2. **Credenciales incorrectas:**
   ```powershell
   # Verifica usuario existe
   curl -X POST http://localhost:8081/api/auth/login `
     -H "Content-Type: application/json" `
     -d '{"username":"admin","password":"Admin@123"}'
   ```

3. **Token expirado:**
   ```javascript
   // DevTools Console
   localStorage.clear()
   // Vuelve a hacer login
   ```

👉 **Ver guía completa:** [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

---

## 📁 ARCHIVOS MODIFICADOS

```
src/
├── app/
│   ├── services/
│   │   ├── auth.ts              ✅ COMPLETO
│   │   ├── user.ts              ✅ ACTUALIZADO + getCurrentUser(), updateUserStatus()
│   │   ├── proveedor.ts         ✅ NUEVO - Endpoints correctos
│   │   └── supplier.ts          (Legacy - no modificado)
│   ├── interceptors/
│   │   └── auth-interceptor.ts  ✅ MEJORADO - Manejo de 401
│   ├── components/
│   │   ├── auth/login/          ✅ Funcional
│   │   ├── users/               ✅ Funcional
│   │   └── suppliers/           ✅ ACTUALIZADO - Usa ProveedorService
│   └── guards/                  ✅ En lugar
├── environments/
│   ├── environment.ts           ✅ Configurado
│   └── environment.prod.ts      ✅ NUEVO
└── models/
    ├── auth.model.ts            ✅ Completeto
    └── supplier.model.ts        ✅ Completo
```

---

## 💡 TIPS

- **Limpiar cache:** `Ctrl+Shift+Del` en navegador
- **DevTools Network:** Filtra por "XHR" para ver requests HTTP
- **DevTools Console:** Pega: `console.log(localStorage)` para ver datos guardados
- **Modo incognito:** Abre en pestaña privada si hay problemas de caché

---

## 📞 RESUMEN DE CAMBIOS CLAVE

✅ **AuthService:** Login, logout, getToken()
✅ **UserService:** getUsers(), getCurrentUser(), updateUserStatus()
✅ **ProveedorService:** NUEVO - getProveedores(), cambiarEstado(), etc.
✅ **HttpInterceptor:** Agrega Bearer token + maneja 401
✅ **SupplierListComponent:** Ahora usa ProveedorService
✅ **environment.prod.ts:** NUEVO

---

**¡Listo! A programar! 🚀**

Cualquier duda: revisa [INTEGRACION_FRONTEND_BACKEND.md](INTEGRACION_FRONTEND_BACKEND.md) o [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
