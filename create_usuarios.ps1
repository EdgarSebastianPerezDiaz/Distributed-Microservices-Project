# ============================================================
# Script para crear usuarios funcionario y auditor
# ============================================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CREACIÓN DE USUARIOS FALTANTES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. LOGIN ADMIN
Write-Host "--- Paso 1: Login Admin ---" -ForegroundColor Yellow
$loginPayload = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

try {
    $loginResp = Invoke-RestMethod -Uri "http://localhost:8084/api/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginPayload
    
    $TOKEN = $loginResp.token
    Write-Host "✅ Login exitoso" -ForegroundColor Green
    Write-Host "   Token obtenido (primeros 50 caracteres): $($TOKEN.Substring(0, 50))..."
} catch {
    Write-Host "❌ Error en login:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit 1
}

Write-Host ""

# 2. CREAR USUARIO FUNCIONARIO
Write-Host "--- Paso 2: Crear Usuario FUNCIONARIO ---" -ForegroundColor Yellow
$funcPayload = @{
    username = "funcionario"
    email = "funcionario@uptc.edu.co"
    password = "funcionario123"
    fullName = "Usuario Funcionario"
    role = "FUNCIONARIO"
} | ConvertTo-Json

try {
    $funcResp = Invoke-RestMethod -Uri "http://localhost:8084/api/auth/register" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $TOKEN"} `
        -Body $funcPayload
    
    Write-Host "✅ Usuario FUNCIONARIO creado:" -ForegroundColor Green
    Write-Host "   ID: $($funcResp.id)"
    Write-Host "   Username: $($funcResp.username)"
    Write-Host "   Email: $($funcResp.email)"
    Write-Host "   Rol: $($funcResp.role)"
    Write-Host "   Activo: $($funcResp.active)"
} catch {
    Write-Host "❌ Error al crear funcionario:" -ForegroundColor Red
    Write-Host "   Status Code: $($_.Exception.Response.StatusCode)"
    Write-Host "   Message: $($_.Exception.Message)"
}

Write-Host ""

# 3. CREAR USUARIO AUDITOR
Write-Host "--- Paso 3: Crear Usuario AUDITOR ---" -ForegroundColor Yellow
$auditorPayload = @{
    username = "auditor"
    email = "auditor@uptc.edu.co"
    password = "auditor123"
    fullName = "Usuario Auditor"
    role = "AUDITOR"
} | ConvertTo-Json

try {
    $auditorResp = Invoke-RestMethod -Uri "http://localhost:8084/api/auth/register" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $TOKEN"} `
        -Body $auditorPayload
    
    Write-Host "✅ Usuario AUDITOR creado:" -ForegroundColor Green
    Write-Host "   ID: $($auditorResp.id)"
    Write-Host "   Username: $($auditorResp.username)"
    Write-Host "   Email: $($auditorResp.email)"
    Write-Host "   Rol: $($auditorResp.role)"
    Write-Host "   Activo: $($auditorResp.active)"
} catch {
    Write-Host "❌ Error al crear auditor:" -ForegroundColor Red
    Write-Host "   Status Code: $($_.Exception.Response.StatusCode)"
    Write-Host "   Message: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACIÓN EN BD" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 4. VERIFICAR EN BD
Write-Host "Usuarios en BD:"
Write-Host ""

$result = psql -U postgres -h localhost -d usuarios_db -c "SELECT username, email, active, role_id FROM users ORDER BY username;"
Write-Host $result

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PRUEBAS DE LOGIN CON NUEVOS USUARIOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 5. TEST LOGIN FUNCIONARIO
Write-Host "--- Test: Login FUNCIONARIO ---" -ForegroundColor Yellow
$funcLoginPayload = @{
    username = "funcionario"
    password = "funcionario123"
} | ConvertTo-Json

try {
    $funcLoginResp = Invoke-RestMethod -Uri "http://localhost:8084/api/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $funcLoginPayload
    
    Write-Host "✅ Login FUNCIONARIO exitoso" -ForegroundColor Green
    Write-Host "   Token: $($funcLoginResp.token.Substring(0, 50))..."
    Write-Host "   Usuario: $($funcLoginResp.user.username)"
    Write-Host "   Email: $($funcLoginResp.user.email)"
    Write-Host "   Rol: $($funcLoginResp.user.role)"
} catch {
    Write-Host "❌ Error en login FUNCIONARIO:" -ForegroundColor Red
    Write-Host "   Status Code: $($_.Exception.Response.StatusCode)"
    Write-Host "   Message: $($_.Exception.Message)"
}

Write-Host ""

# 6. TEST LOGIN AUDITOR
Write-Host "--- Test: Login AUDITOR ---" -ForegroundColor Yellow
$auditorLoginPayload = @{
    username = "auditor"
    password = "auditor123"
} | ConvertTo-Json

try {
    $auditorLoginResp = Invoke-RestMethod -Uri "http://localhost:8084/api/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $auditorLoginPayload
    
    Write-Host "✅ Login AUDITOR exitoso" -ForegroundColor Green
    Write-Host "   Token: $($auditorLoginResp.token.Substring(0, 50))..."
    Write-Host "   Usuario: $($auditorLoginResp.user.username)"
    Write-Host "   Email: $($auditorLoginResp.user.email)"
    Write-Host "   Rol: $($auditorLoginResp.user.role)"
} catch {
    Write-Host "❌ Error en login AUDITOR:" -ForegroundColor Red
    Write-Host "   Status Code: $($_.Exception.Response.StatusCode)"
    Write-Host "   Message: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "✅ COMPLETADO" -ForegroundColor Cyan
