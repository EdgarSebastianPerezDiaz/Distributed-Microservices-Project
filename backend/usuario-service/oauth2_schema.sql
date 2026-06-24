-- =============================================================================
-- OAUTH 2.0 REFRESH TOKENS TABLE
-- Extensión de la arquitectura de autenticación existente (JWT Legacy + OAuth2)
-- =============================================================================
-- Propósito: Almacenar refresh tokens de OAuth 2.0 en PostgreSQL
-- Ciclo de vida: Se crean al autenticar, se revocan al logout, se expiran automáticamente
-- Tabla de auditoría para cumplimiento normativo y seguridad
-- =============================================================================

-- Drop existing table if exists (para deployments limpios en desarrollo)
DROP TABLE IF EXISTS oauth_refresh_tokens CASCADE;

-- =============================================================================
-- OAUTH REFRESH TOKENS TABLE
-- =============================================================================
-- Almacena tokens de actualización (refresh tokens) de OAuth 2.0
-- Un refresh token permite obtener un nuevo access token sin re-autenticar
-- Tiempo de vida: 7 días (configurable en AuthorizationServerConfig)
-- =============================================================================
CREATE TABLE IF NOT EXISTS oauth_refresh_tokens (
    -- Identificador único del token
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Valor del token (almacenado en BD, no en memoria)
    -- Se utiliza para validar cuando el cliente lo envía al endpoint /oauth2/token
    token_value TEXT NOT NULL UNIQUE,
    
    -- Referencia al usuario propietario del token
    -- Permite revocar todos los tokens de un usuario al cambiar contraseña
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- ID del cliente OAuth que pidió el token (frontend-app, microservices-client, etc.)
    -- Útil para auditoria y para diferencias entre clientes
    client_id VARCHAR(100) NOT NULL,
    
    -- Fecha y hora de expiración del refresh token
    -- Después de esto, no se puede usar para obtener nuevos access tokens
    expires_at TIMESTAMP NOT NULL,
    
    -- Flag de revocación (soft delete)
    -- true = token revocado por usuario o admin
    -- false = token activo y aún válido
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Auditoría: Cuándo se creó el token (logout de sesión anterior)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Auditoría: Cuándo se actualizó por última vez
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- INDEXES PARA PERFORMANCE
-- =============================================================================
-- Objetivo: Respuesta < 2ms para queries frecuentes

-- Index 1: Búsqueda rápida por valor de token (usado en validación)
-- Query: SELECT * FROM oauth_refresh_tokens WHERE token_value = ?
CREATE INDEX IF NOT EXISTS idx_oauth_refresh_tokens_token_value 
    ON oauth_refresh_tokens(token_value);

-- Index 2: Búsqueda rápida por user_id (usado en logout de todos los dispositivos)
-- Query: SELECT * FROM oauth_refresh_tokens WHERE user_id = ? AND revoked = false
CREATE INDEX IF NOT EXISTS idx_oauth_refresh_tokens_user_id 
    ON oauth_refresh_tokens(user_id) WHERE revoked = FALSE;

-- Index 3: Búsqueda de tokens expirados (usado en limpieza periódica)
-- Query: DELETE FROM oauth_refresh_tokens WHERE expires_at < NOW()
CREATE INDEX IF NOT EXISTS idx_oauth_refresh_tokens_expires_at 
    ON oauth_refresh_tokens(expires_at) WHERE revoked = FALSE;

-- Index 4: Búsqueda por cliente (auditoría y estadísticas)
-- Query: SELECT * FROM oauth_refresh_tokens WHERE client_id = ?
CREATE INDEX IF NOT EXISTS idx_oauth_refresh_tokens_client_id 
    ON oauth_refresh_tokens(client_id);

-- Index 5: Búsqueda de tokens activos no expirados (validación en tiempo real)
-- Query: SELECT * FROM oauth_refresh_tokens WHERE user_id = ? AND revoked = false AND expires_at > NOW()
CREATE INDEX IF NOT EXISTS idx_oauth_refresh_tokens_active 
    ON oauth_refresh_tokens(user_id, revoked, expires_at);

-- =============================================================================
-- VERIFICACIÓN DE INTEGRIDAD (CONSTRAINTS)
-- =============================================================================

-- Validar que el token_value no esté vacío
ALTER TABLE oauth_refresh_tokens 
    ADD CONSTRAINT token_value_not_empty 
    CHECK (LENGTH(TRIM(token_value)) > 0);

-- Validar que expires_at sea en el futuro al crear
-- (Se puede comentar si se necesita crear tokens con fecha pasada para testing)
-- ALTER TABLE oauth_refresh_tokens 
--     ADD CONSTRAINT expires_at_future 
--     CHECK (expires_at > CURRENT_TIMESTAMP);

-- =============================================================================
-- TRIGGERS PARA AUTOMATIZACIÓN (OPCIONAL)
-- =============================================================================
-- Trigger para actualizar updated_at automáticamente

CREATE OR REPLACE FUNCTION update_oauth_refresh_tokens_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_oauth_refresh_tokens_updated_at ON oauth_refresh_tokens;

CREATE TRIGGER trigger_oauth_refresh_tokens_updated_at
    BEFORE UPDATE ON oauth_refresh_tokens
    FOR EACH ROW
    EXECUTE FUNCTION update_oauth_refresh_tokens_updated_at();

-- =============================================================================
-- VISTAS ÚTILES PARA REPORTING Y AUDITORÍA
-- =============================================================================

-- Vista 1: Tokens activos por usuario
CREATE OR REPLACE VIEW vw_active_oauth_tokens AS
SELECT 
    ort.id,
    ort.user_id,
    u.username,
    ort.client_id,
    ort.created_at,
    ort.expires_at,
    (ort.expires_at - CURRENT_TIMESTAMP) as time_remaining,
    ort.revoked
FROM oauth_refresh_tokens ort
JOIN users u ON ort.user_id = u.id
WHERE ort.revoked = FALSE AND ort.expires_at > CURRENT_TIMESTAMP;

-- Vista 2: Historial de tokens (incluyendo revocados)
CREATE OR REPLACE VIEW vw_oauth_tokens_history AS
SELECT 
    ort.id,
    ort.user_id,
    u.username,
    ort.client_id,
    ort.created_at,
    ort.expires_at,
    ort.revoked,
    CASE 
        WHEN ort.revoked THEN 'REVOKED'
        WHEN ort.expires_at < CURRENT_TIMESTAMP THEN 'EXPIRED'
        ELSE 'ACTIVE'
    END as status
FROM oauth_refresh_tokens ort
JOIN users u ON ort.user_id = u.id
ORDER BY ort.created_at DESC;

-- =============================================================================
-- DATOS DE PRUEBA (OPCIONAL - COMENTAR EN PRODUCCIÓN)
-- =============================================================================
-- Descomenta esto solo si necesitas datos de prueba para desarrollo

/*
-- Ejemplo: Crear un refresh token de prueba para el usuario admin
-- Nota: Este es solo un ejemplo, los tokens reales se generan en OAuth2RefreshTokenService

-- Asumiendo que admin existe con user_id = 550e8400-e29b-41d4-a716-446655440000
-- INSERT INTO oauth_refresh_tokens (token_value, user_id, client_id, expires_at)
-- VALUES (
--     'test_refresh_token_12345678901234567890',
--     '550e8400-e29b-41d4-a716-446655440000',
--     'frontend-app',
--     CURRENT_TIMESTAMP + INTERVAL '7 days'
-- );
*/

-- =============================================================================
-- COMENTARIOS FINALES
-- =============================================================================
-- Ejecutar este script una sola vez en la base de datos usuarios_db
-- Comando: psql -U postgres -d usuarios_db -f oauth2_schema.sql
--
-- Requisitos previos:
-- 1. PostgreSQL 12+ debe estar ejecutándose
-- 2. Base de datos "usuarios_db" debe existir
-- 3. Tabla "users" debe existir (con columna id UUID como PK)
--
-- Verificación posterior a la ejecución:
-- SELECT * FROM oauth_refresh_tokens;
-- SELECT * FROM vw_active_oauth_tokens;
-- =============================================================================
