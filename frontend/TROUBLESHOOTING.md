# 🔍 TROUBLESHOOTING GUIDE - CONEXIÓN FRONTEND/BACKEND

**Última actualización:** May 19, 2026

---

## ❌ Problema: "No se pudo conectar al servidor"

**Síntomas:**
- Error en login: "No se pude conectar al servidor"
- Network tab muestra: Status `0` o `Failed`
- Console log: `error.status === 0`

**Soluciones:**

1. **Verificar que backend está corriendo:**
   ```powershell
   # Verifica que el API Gateway está activo
   curl http://localhost:8081/actuator/health
   
   # Resultado esperado: {"status":"UP"}
   ```

2. **Verificar puerto 8081:**
   ```powershell
   netstat -ano | findstr ":8081"
   # Debería mostrar un proceso escuchando
   ```

3. **Limpiar caché de Angular:**
   ```powershell
   # Termina ng serve (Ctrl+C)
   rm -r .angular
   ng serve --open
   ```

4. **CORS (si es en producción):**
   - Backend debe tener CORS habilitado para origen del frontend
   - En `application.yaml` backend:
   ```yaml
   cors:
     allowed-origins: http://localhost:4200,https://example.com
     allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
     allowed-headers: Authorization,Content-Type
   ```

---

## ❌ Problema: Error 401 Unauthorized en login

**Síntomas:**
- Login: "Credenciales inválidas"
- Network: POST `/api/auth/login` retorna 401
- Body error: `"Invalid credentials"` o similar

**Soluciones:**

1. **Verificar credenciales:**
   ```powershell
   # Default admin del backend:
   # Username: admin
   # Password: Admin@123
   
   # Prueba manualmente:
   curl -X POST http://localhost:8081/api/auth/login `
     -H "Content-Type: application/json" `
     -d '{"username":"admin","password":"Admin@123"}'
   ```

2. **Verificar que usuario existe en BD:**
   ```bash
   # Conectarse a PostgreSQL
   psql -U postgres -d usuarios_db -c "SELECT * FROM users WHERE username='admin';"
   ```

3. **Verificar usuario está activo:**
   ```bash
   psql -U postgres -d usuarios_db -c "SELECT * FROM users WHERE username='admin';" 
   # Asegurar que `active` = true
   ```

4. **Crear usuario de prueba si no existe:**
   ```bash
   psql -U postgres -d usuarios_db << EOF
   INSERT INTO users (username, email, password_hash, role, active, created_at)
   VALUES ('admin', 'admin@example.com', '$2a$10$...', 'ADMINISTRADOR', true, NOW());
   EOF
   ```

---

## ❌ Problema: Token no se guarda en localStorage

**Síntomas:**
- Después de login, localStorage no tiene `token`
- Próxima recarga: automáticamente va a login
- Network: `/api/auth/login` retorna 200 pero sin token

**Verificar:**

1. **DevTools → Application → LocalStorage → http://localhost:4200**
   - Debe haber entrada con key: `token`
   - Value: JWT largo (comienza con `eyJ...`)

2. **Verificar response del backend:**
   - DevTools → Network → `/api/auth/login` → Response
   - Debe incluir: `{ "token": "...", "user": {...} }`

3. **Verificar AuthService:**
   ```typescript
   // src/app/services/auth.ts
   login(credentials).subscribe({
     next: (response) => {
       console.log('Response del servidor:', response);
       // Debe mostrar objeto con token y user
     }
   });
   ```

4. **Limpiar localStorage y reintentar:**
   ```javascript
   // DevTools Console
   localStorage.clear();
   // Luego intenta login de nuevo
   ```

---

## ❌ Problema: 403 Forbidden en requests protegidos

**Síntomas:**
- Login funciona ✅
- GET `/api/auth/users` retorna 403
- Network: Authorization header presente pero rechazado

**Causa probable:** Token expirado o rol insuficiente

**Soluciones:**

1. **Verificar token es válido:**
   ```javascript
   // DevTools Console
   const token = localStorage.getItem('token');
   console.log('Token:', token);
   // Debe ser un JWT válido (3 partes separadas por puntos)
   ```

2. **Decodificar JWT para ver expiration:**
   ```javascript
   // Usar https://jwt.io en navegador
   // Pega el token y verifica:
   // - exp: timestamp de expiración
   // - rol: debe coincidir con usuario
   ```

3. **Si token expiró (después de 24h):**
   - Hacer logout: `authService.logout()`
   - Volver a hacer login

4. **Si usuario no tiene permisos (403 persistente):**
   - Verificar rol en BD:
   ```bash
   psql -U postgres -d usuarios_db -c "SELECT role FROM users WHERE username='admin';"
   # Debe ser: ADMINISTRADOR
   ```

---

## ❌ Problema: Listar usuarios muestra lista vacía

**Síntomas:**
- Componente `user-list` carga pero tabla vacía
- Network: GET `/api/auth/users` retorna 200
- Response: `{ "content": [] }`

**Soluciones:**

1. **Verificar BD tiene usuarios:**
   ```bash
   psql -U postgres -d usuarios_db -c "SELECT COUNT(*) FROM users;"
   # Resultado: count > 0
   ```

2. **Crear usuario de prueba:**
   ```bash
   # En backend, hacer POST /api/auth/register
   curl -X POST http://localhost:8081/api/auth/register \
     -H "Authorization: Bearer <ADMIN_TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
       "username": "test",
       "email": "test@example.com",
       "password": "Test@123",
       "role": "FUNCIONARIO"
     }'
   ```

3. **Verificar paginación:**
   - Backend puede retornar página 0 vacía si todos están en página > 0
   - Revisar query params: `?page=0&size=10`

---

## ❌ Problema: Listar proveedores muestra error "Cannot read property 'content'"

**Síntomas:**
- Console error: "Cannot read property 'content' of undefined"
- Network: GET `/api/proveedores` retorna 200
- Response es array directo, no objeto con "content"

**Causa:** Backend retorna estructura diferente a la esperada

**Soluciones:**

1. **Verificar estructura en Network tab:**
   - GET `/api/proveedores` → Response
   - Si es array directo: `[{id:1,...}, {id:2,...}]`
   - Si es objeto: `{ "content": [...], "totalElements": 10 }`

2. **Actualizar componente según respuesta:**
   ```typescript
   // src/app/components/suppliers/supplier-list/supplier-list.ts
   loadProveedores() {
     this.proveedorService.getProveedores().subscribe({
       next: (response) => {
         // Opción 1: Si retorna objeto con "content"
         this.proveedores = response.content || [];
         
         // Opción 2: Si retorna array directo
         this.proveedores = Array.isArray(response) ? response : response.content || [];
         
         // Opción 3: Si retorna estructura completamente diferente
         console.log('Response estructura:', response);
       }
     });
   }
   ```

3. **Actualizar ProveedorService si es necesario:**
   ```typescript
   // src/app/services/proveedor.ts
   getProveedores(page?: number, pageSize?: number, search?: string) {
     // Si backend NO soporta paginación:
     return this.http.get<any>(`${this.apiUrl}/api/proveedores`);
     
     // Si backend SÍ soporta paginación:
     let params = new HttpParams()
       .set('page', (page || 0).toString())
       .set('size', (pageSize || 10).toString());
     return this.http.get<any>(`${this.apiUrl}/api/proveedores`, { params });
   }
   ```

---

## ❌ Problema: Cambiar estado de proveedor no funciona

**Síntomas:**
- Click en botón estado no hace nada
- Network: No hay PATCH request
- Console: Error o silencioso

**Soluciones:**

1. **Verificar endpoint exacto del backend:**
   ```powershell
   # Hacer request manual
   $token = "tu_jwt_token_aqui"
   curl -X PATCH http://localhost:8081/api/proveedores/1/estado `
     -H "Authorization: Bearer $token" `
     -H "Content-Type: application/json" `
     -d '{"estado":"INACTIVO"}'
   # Debería retornar 200 con proveedor actualizado
   ```

2. **Si retorna 404 (Not Found):**
   - Verificar que ID del proveedor existe
   - Verificar endpoint: `/api/proveedores/{id}/estado` (no `/suppliers/`)

3. **Si retorna 400 (Bad Request):**
   - Verificar formato del body:
   ```typescript
   // Correcto:
   { estado: "ACTIVO" } o { estado: "INACTIVO" }
   
   // Incorrecto:
   { status: "ACTIVE" }  // Campo nombre incorrecto
   { estado: true }       // Tipo incorrecto (debe ser string)
   ```

4. **Verificar método en ProveedorService:**
   ```typescript
   cambiarEstado(id: string, estado: 'ACTIVO' | 'INACTIVO'): Observable<Proveedor> {
     return this.http.patch<Proveedor>(
       `${this.apiUrl}/api/proveedores/${id}/estado`,
       { estado }
     ).pipe(
       catchError((error) => {
         console.error('Error:', error);
         return throwError(() => error);
       })
     );
   }
   ```

---

## ✅ Verificación de Sistema Funcionando

**Checklist para confirmar que todo está bien:**

```typescript
// Abre DevTools Console y ejecuta:

// 1. ¿Token guardado?
console.log('Token:', localStorage.getItem('token') ? '✅ Sí' : '❌ No');

// 2. ¿Usuario guardado?
console.log('Usuario:', localStorage.getItem('user') ? '✅ Sí' : '❌ No');

// 3. ¿Backend responde?
fetch('http://localhost:8081/actuator/health')
  .then(r => r.json())
  .then(data => console.log('Backend status:', data.status))
  .catch(() => console.log('❌ Backend no responde'));

// 4. ¿Interceptor agrega header?
// (Revisar en Network tab: Authorization header debe estar en requests)

// 5. ¿API retorna datos?
const token = localStorage.getItem('token');
fetch('http://localhost:8081/api/auth/users', {
  headers: { 'Authorization': `Bearer ${token}` }
})
  .then(r => r.json())
  .then(data => console.log('Usuarios:', data))
  .catch(e => console.log('Error:', e));
```

---

## 🔧 Commands Útiles para Debugging

```powershell
# Limpiar Angular cache
rm -r .angular

# Reiniciar desarrollo
ng serve --open

# Resetear localStorage (desde Console)
localStorage.clear()

# Ver eventos HTTP en tiempo real
# Network tab → Filter "XHR"

# Ver estado de autenticación
# Application → LocalStorage → http://localhost:4200

# Decodificar JWT (desde Console)
JSON.parse(atob(localStorage.getItem('token').split('.')[1]))

# Crear una petición manual para debugging
curl -X GET http://localhost:8081/api/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📊 MATRIZ DE ERRORES COMUNES

| Código | Significado | Solución |
|--------|------------|----------|
| 0 | Backend no disponible | Iniciar microservicios |
| 200 | Éxito | ✅ Todo bien |
| 400 | Bad Request | Verificar datos enviados |
| 401 | Unauthorized | Revisar token/credenciales |
| 403 | Forbidden | Verificar rol/permisos |
| 404 | Not Found | Endpoint o ID incorrecto |
| 500 | Server Error | Revisar logs del backend |

---

**¡Encuentra el problema y resuelto!** 🎯
