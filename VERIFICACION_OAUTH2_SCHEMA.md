# ✅ Verificación de Schema OAuth 2.0

## Script Corregido: oauth2_schema.sql

El script `backend/usuario-service/oauth2_schema.sql` ha sido actualizado para usar tu base de datos real:

### Cambios Realizados

| Elemento | Anterior | Nuevo |
|----------|----------|-------|
| **Tabla de usuarios** | `usuarios` | `users` |
| **PK en foreign key** | `usuarios(id)` | `users(id)` |
| **JOIN en vista 1** | `ON ort.user_id = u.user_id` | `ON ort.user_id = u.id` |
| **JOIN en vista 2** | `ON ort.user_id = u.user_id` | `ON ort.user_id = u.id` |

---

## 🔍 Paso 1: Verificar la Tabla `users` Existente

Antes de ejecutar el script, verifica que tu tabla `users` existe y tiene la estructura correcta:

```sql
-- Conectarse a la BD
psql -U postgres -d usuarios_db

-- Ejecutar estas consultas:
\dt users;  -- Mostrar estructura
SELECT column_name, data_type FROM information_schema.columns WHERE table_name='users' ORDER BY ordinal_position;
```

**Resultado esperado:**
```
        Column        |       Type        | Nullable
---------------------+-------------------+----------
 id                  | uuid              | NO
 username            | character varying | NO
 email               | character varying | NO
 password            | character varying | NO
 (... otras columnas)
```

---

## 🚀 Paso 2: Ejecutar el Script Corregido

### Opción A: Ejecución Directa (Recomendado)

```bash
# Desde el directorio del proyecto
psql -U postgres -d usuarios_db -f backend/usuario-service/oauth2_schema.sql
```

**Resultado esperado:**
```
DROP TABLE
CREATE TABLE
CREATE INDEX
CREATE INDEX
CREATE INDEX
CREATE INDEX
CREATE INDEX
ALTER TABLE
CREATE FUNCTION
CREATE TRIGGER
CREATE VIEW
CREATE VIEW
```

### Opción B: Ejecución Paso a Paso (Más Seguro)

Si prefieres mayor control, ejecuta en psql interactivamente:

```bash
psql -U postgres -d usuarios_db
```

Luego dentro de psql:

```sql
-- 1. Ver las tablas antes
\dt

-- 2. Ejecutar el script
\i backend/usuario-service/oauth2_schema.sql

-- 3. Verificar que se creó correctamente
SELECT * FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name = 'oauth_refresh_tokens';

-- 4. Ver las vistas creadas
SELECT * FROM information_schema.views 
WHERE table_schema = 'public' 
AND table_name LIKE 'vw_%';

-- 5. Salir
\q
```

---

## ✅ Paso 3: Verificación Post-Ejecución

Ejecuta estas consultas para confirmar que todo está correctamente creado:

```sql
-- 1. ¿Existe la tabla?
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' AND table_name = 'oauth_refresh_tokens';
-- Resultado esperado: 1 fila con 'oauth_refresh_tokens'

-- 2. ¿Tiene 8 columnas?
SELECT COUNT(*) as column_count FROM information_schema.columns 
WHERE table_name = 'oauth_refresh_tokens';
-- Resultado esperado: 8

-- 3. ¿Tiene 5 índices?
SELECT COUNT(*) as index_count FROM information_schema.indexes 
WHERE tablename = 'oauth_refresh_tokens' AND schemaname = 'public';
-- Resultado esperado: 6 (incluyendo PK)

-- 4. ¿Están las vistas creadas?
SELECT viewname FROM information_schema.views 
WHERE schemaname = 'public' AND viewname LIKE 'vw_oauth%';
-- Resultado esperado: 2 filas (vw_active_oauth_tokens, vw_oauth_tokens_history)

-- 5. ¿Funciona el trigger?
SELECT trigger_name FROM information_schema.triggers 
WHERE event_object_table = 'oauth_refresh_tokens';
-- Resultado esperado: 1 fila (trigger_oauth_refresh_tokens_updated_at)

-- 6. ¿Funciona la FK (relación con users)?
\d oauth_refresh_tokens
-- Resultado esperado: Ver "Foreign-key constraints" apuntando a users(id)
```

---

## 🔐 Script de Verificación Completo

Crea un archivo `verify_oauth_schema.sql`:

```sql
-- Verificación completa del schema OAuth2

\echo '=== VERIFICACIÓN DE SCHEMA OAUTH 2.0 ==='
\echo ''

\echo '1. Tabla oauth_refresh_tokens:'
SELECT table_name, table_schema FROM information_schema.tables 
WHERE table_name = 'oauth_refresh_tokens';
\echo ''

\echo '2. Columnas de la tabla:'
\d oauth_refresh_tokens
\echo ''

\echo '3. Índices creados:'
SELECT indexname, indexdef FROM pg_indexes 
WHERE tablename = 'oauth_refresh_tokens' ORDER BY indexname;
\echo ''

\echo '4. Vistas creadas:'
SELECT viewname FROM information_schema.views 
WHERE viewname LIKE 'vw_oauth%' ORDER BY viewname;
\echo ''

\echo '5. Trigger creado:'
SELECT trigger_name FROM information_schema.triggers 
WHERE event_object_table = 'oauth_refresh_tokens';
\echo ''

\echo '6. Constraint de clave foránea (debe apuntar a users.id):'
SELECT constraint_name, table_name, column_name, foreign_table_name, foreign_column_name
FROM information_schema.key_column_usage
WHERE table_name = 'oauth_refresh_tokens' AND constraint_type = 'FOREIGN KEY';
\echo ''

\echo '=== FIN DE VERIFICACIÓN ==='
```

Ejecutar:
```bash
psql -U postgres -d usuarios_db -f verify_oauth_schema.sql
```

---

## ⚠️ Troubleshooting

### Error: "relation 'users' does not exist"
```
ERROR: relation "users" does not exist
```
**Solución:** Verifica que la tabla `users` existe:
```bash
psql -U postgres -d usuarios_db -c "\dt users"
```
Si no existe, primero ejecuta `backend/docs/database-models/users.sql`

### Error: "user_id already exists"
```
ERROR: column "user_id" of relation "oauth_refresh_tokens" already exists
```
**Solución:** La tabla ya existe. Limpia primero:
```sql
DROP TABLE IF EXISTS oauth_refresh_tokens CASCADE;
-- Luego ejecuta el script de nuevo
```

### Error: "FK constraint violation"
```
ERROR: insert or update on table "oauth_refresh_tokens" violates foreign key constraint
```
**Solución:** Asegúrate que el usuario existe en `users` antes de insertar refresh tokens

### Vista muestra "u.username" incorrecto
Si `users` no tiene columna `username`, cambia en las vistas:
```sql
-- En vw_active_oauth_tokens y vw_oauth_tokens_history, reemplaza:
u.username  -- con lo que tu tabla tenga (ej: u.user_name, u.first_name, etc.)
```

---

## 📋 Checklist Final

- [ ] Tabla `users` verificada
- [ ] Script `oauth2_schema.sql` ejecutado sin errores
- [ ] Tabla `oauth_refresh_tokens` creada
- [ ] 5 índices creados
- [ ] 2 vistas creadas (`vw_active_oauth_tokens`, `vw_oauth_tokens_history`)
- [ ] Trigger `trigger_oauth_refresh_tokens_updated_at` creado
- [ ] FK apunta correctamente a `users(id)`
- [ ] Consulta de test retorna resultados (o está vacía, es normal)

---

## 🔗 Próximos Pasos

Una vez verificado el schema:

1. ✅ Compilar usuario-service:
   ```bash
   cd backend/usuario-service/usuario-service
   mvn clean package -DskipTests
   ```

2. ✅ Iniciar los servicios (en orden):
   - Eureka Server (8761)
   - Usuario Service (8084)
   - API Gateway (8081)

3. ✅ Ejecutar pruebas:
   ```bash
   chmod +x test-oauth.sh
   ./test-oauth.sh
   ```

---

**Documento generado**: Mayo 2026  
**Versión**: 1.0
