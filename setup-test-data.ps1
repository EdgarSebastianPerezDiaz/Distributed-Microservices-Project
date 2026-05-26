# Script para crear datos de prueba
# Asegurate de que los servicios de backend estan corriendo

$baseUrl = "http://localhost:8081"
$adminToken = ""

Write-Host "=== Creando datos de prueba ===" -ForegroundColor Green
Write-Host ""

# 1. Login como admin
Write-Host "1. Intentando login como admin..." -ForegroundColor Yellow

$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody `
        -ErrorAction Stop

    $loginData = $loginResponse.Content | ConvertFrom-Json
    $adminToken = $loginData.token
    Write-Host "OK - Login exitoso" -ForegroundColor Green
} catch {
    Write-Host "ERROR - Login fallido" $_.Exception.Message -ForegroundColor Red
    exit
}

# 2. Crear usuario FUNCIONARIO
Write-Host "2. Creando usuario FUNCIONARIO..." -ForegroundColor Yellow

$userBody = @{
    username = "funcionario01"
    password = "funcionario123"
    email = "funcionario@test.local"
    fullName = "Juan Funcionario"
    role = "FUNCIONARIO"
} | ConvertTo-Json

try {
    Invoke-WebRequest -Uri "$baseUrl/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $adminToken"} `
        -Body $userBody `
        -ErrorAction Stop | Out-Null
    Write-Host "OK" -ForegroundColor Green
} catch {
    Write-Host "ERROR" $_.Exception.Message -ForegroundColor Red
}

# 3. Crear usuario AUDITOR
Write-Host "3. Creando usuario AUDITOR..." -ForegroundColor Yellow

$auditorBody = @{
    username = "auditor01"
    password = "auditor123"
    email = "auditor@test.local"
    fullName = "Maria Auditora"
    role = "AUDITOR"
} | ConvertTo-Json

try {
    Invoke-WebRequest -Uri "$baseUrl/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $adminToken"} `
        -Body $auditorBody `
        -ErrorAction Stop | Out-Null
    Write-Host "OK" -ForegroundColor Green
} catch {
    Write-Host "ERROR" $_.Exception.Message -ForegroundColor Red
}

# 4. Crear proveedor 1
Write-Host "4. Creando proveedor de prueba..." -ForegroundColor Yellow

$supplierBody = @{
    nit = "123456789"
    businessName = "Proveedor Tecnologico S.A.S."
    email = "contacto@proveedor.local"
    phone = "+57 1 2345678"
    personType = "JURIDICA"
    estado = "ACTIVO"
} | ConvertTo-Json

try {
    Invoke-WebRequest -Uri "$baseUrl/api/proveedores" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $adminToken"} `
        -Body $supplierBody `
        -ErrorAction Stop | Out-Null
    Write-Host "OK" -ForegroundColor Green
} catch {
    Write-Host "ERROR" $_.Exception.Message -ForegroundColor Red
}

# 5. Crear proveedor 2
Write-Host "5. Creando segundo proveedor..." -ForegroundColor Yellow

$supplier2Body = @{
    nit = "987654321"
    businessName = "Servicios Generales LTDA"
    email = "info@servicios.local"
    phone = "+57 1 8765432"
    personType = "NATURAL"
    estado = "ACTIVO"
} | ConvertTo-Json

try {
    Invoke-WebRequest -Uri "$baseUrl/api/proveedores" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{"Authorization" = "Bearer $adminToken"} `
        -Body $supplier2Body `
        -ErrorAction Stop | Out-Null
    Write-Host "OK" -ForegroundColor Green
} catch {
    Write-Host "ERROR" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "Credenciales de prueba:" -ForegroundColor Cyan
Write-Host "Admin: admin / admin123"
Write-Host "Funcionario: funcionario01 / funcionario123"
Write-Host "Auditor: auditor01 / auditor123"
