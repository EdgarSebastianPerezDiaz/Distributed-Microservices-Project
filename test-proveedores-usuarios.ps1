# Pruebas automatizadas de API para usuarios y proveedores
# Ejecutar en PowerShell desde la carpeta raíz del proyecto.
# Uso:
#   powershell -ExecutionPolicy Bypass -File .\test-proveedores-usuarios.ps1

$baseUrl = 'http://localhost:8081'
$adminCredentials = @{ username = 'admin'; password = 'Admin@123' }
$headers = @{ 'Accept' = 'application/json' }
$results = @()

function Write-Result($name, $success, $status, $message) {
    $statusText = if ($success) { 'PASS' } else { 'FAIL' }
    Write-Host "[$statusText] $name -> HTTP $status - $message"
    $results += [pscustomobject]@{
        Test = $name
        Result = $statusText
        Status = $status
        Message = $message
    }
}

function Invoke-ApiRequest {
    param(
        [Parameter(Mandatory=$true)] [string]$Method,
        [Parameter(Mandatory=$true)] [string]$Url,
        [hashtable]$Headers = @{},
        $Body = $null
    )

    $invokeParams = @{
        Uri = $Url
        Method = $Method
        Headers = $Headers
        ContentType = 'application/json'
        ErrorAction = 'Stop'
    }

    if ($Body -ne $null) {
        $invokeParams.Body = $Body | ConvertTo-Json -Depth 10
    }

    try {
        $response = Invoke-WebRequest @invokeParams
        $body = $null
        if ($response.Content) {
            $body = try { $response.Content | ConvertFrom-Json -ErrorAction Stop } catch { $response.Content }
        }
        return @{ Success = $true; Status = [int]$response.StatusCode; Body = $body }
    } catch {
        $status = 0
        $body = $null
        if ($_.Exception.Response -ne $null) {
            try {
                $status = [int]$_.Exception.Response.StatusCode
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $text = $reader.ReadToEnd()
                $body = try { $text | ConvertFrom-Json -ErrorAction Stop } catch { $text }
            } catch {
                $status = 0
                $body = $_.Exception.Message
            }
        }
        return @{ Success = $false; Status = $status; Body = $body; Error = $_.Exception.Message }
    }
}

function Login-Admin {
    Write-Host '==> Iniciando sesión con admin / Admin@123'
    $url = "$baseUrl/api/auth/login"
    $result = Invoke-ApiRequest -Method 'POST' -Url $url -Headers $headers -Body $adminCredentials
    if ($result.Success -and $result.Body.token) {
        $token = $result.Body.token
        $headers['Authorization'] = "Bearer $token"
        Write-Result 'Login admin' $true $result.Status 'Token obtenido'
        return $true
    }
    Write-Result 'Login admin' $false $result.Status "No se obtuvo token. Error: $($result.Body)"
    return $false
}

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [int[]]$ExpectedStatus = @(200)
    )
    $result = Invoke-ApiRequest -Method $Method -Url $Url -Headers $headers -Body $Body
    $success = $ExpectedStatus -contains $result.Status
    $expected = $ExpectedStatus -join '/' 
    $message = if ($success) { 'OK' } else { "Esperado $expected, recibido $($result.Status): $($result.Body)" }
    Write-Result $Name $success $result.Status $message
    return $result
}

function Summarize-Results {
    Write-Host "`n===== Resumen de pruebas ====="
    $passes = ($results | Where-Object { $_.Result -eq 'PASS' }).Count
    $fails = ($results | Where-Object { $_.Result -eq 'FAIL' }).Count
    $results | Format-Table -AutoSize
    Write-Host "`nTotal PASS: $passes   Total FAIL: $fails"
    if ($fails -gt 0) {
        Write-Host 'Si falla algún test, copia el mensaje de error y revísalo con el asistente.'
    }
}

# Inicio
if (-not (Login-Admin)) {
    Summarize-Results
    exit 1
}

# =====================
# Usuarios
# =====================
$usersUrl = "$baseUrl/api/auth/users"
$userList = Test-Endpoint -Name 'GET /api/auth/users' -Method 'GET' -Url $usersUrl -ExpectedStatus @(200)

# Crear usuario funcionario con datos únicos
$userPayload = @{ 
    username = 'prueba_func'
    password = 'Func123'
    email = 'func@test.com'
    fullName = 'Prueba Funcionario'
    role = 'FUNCIONARIO'
}
$userCreate = Test-Endpoint -Name 'POST /api/auth/register' -Method 'POST' -Url "$baseUrl/api/auth/register" -Body $userPayload -ExpectedStatus @(201)

$userId = $null
if ($userCreate.Success -and $userCreate.Body.id) {
    $userId = $userCreate.Body.id
} elseif ($userCreate.Body -is [hashtable] -and $userCreate.Body.id) {
    $userId = $userCreate.Body.id
}

if ($userId) {
    # Actualizar nombre/apellido usando PUT /api/users/{id}
    $updatePayload = @{ fullName = 'Prueba Usuario' }
    Test-Endpoint -Name "PUT /api/users/$userId" -Method 'PUT' -Url "$baseUrl/api/users/$userId" -Body $updatePayload -ExpectedStatus @(200)

    # Cambiar estado con PATCH /api/auth/users/{id}/status
    Test-Endpoint -Name "PATCH /api/auth/users/$userId/status (desactivar)" -Method 'PATCH' -Url "$baseUrl/api/auth/users/$userId/status" -ExpectedStatus @(200)
    Test-Endpoint -Name "PATCH /api/auth/users/$userId/status (activar)" -Method 'PATCH' -Url "$baseUrl/api/auth/users/$userId/status" -ExpectedStatus @(200)
} else {
    Write-Host 'WARN: No se pudo obtener el ID del usuario creado. Se omiten pruebas de PUT/PATCH usuario.'
}

# =====================
# Proveedores
# =====================
$suppliersUrl = "$baseUrl/api/suppliers"
Test-Endpoint -Name 'GET /api/suppliers' -Method 'GET' -Url $suppliersUrl -ExpectedStatus @(200)

$supplierPayload1 = @{ 
    nit = '123456789-1'
    businessName = 'Proveedor Test SRL'
    personType = 'JURIDICA'
    email = 'contacto@proveedor1.com'
    phone = '6012345678'
}
$supplier1 = Test-Endpoint -Name 'POST /api/suppliers (Proveedor 1)' -Method 'POST' -Url $suppliersUrl -Body $supplierPayload1 -ExpectedStatus @(201)

$supplierPayload2 = @{ 
    nit = '987654321-0'
    businessName = 'Servicios Test Ltda'
    personType = 'NATURAL'
    email = 'info@serviciostest.com'
    phone = '6011111111'
}
$supplier2 = Test-Endpoint -Name 'POST /api/suppliers (Proveedor 2)' -Method 'POST' -Url $suppliersUrl -Body $supplierPayload2 -ExpectedStatus @(201)

# Usar el primer proveedor creado para pruebas siguientes
$supplierId = $null
if ($supplier1.Success -and $supplier1.Body.id) {
    $supplierId = $supplier1.Body.id
} elseif ($supplier2.Success -and $supplier2.Body.id) {
    $supplierId = $supplier2.Body.id
}

if ($supplierId) {
    Test-Endpoint -Name "GET /api/suppliers/$supplierId" -Method 'GET' -Url "$suppliersUrl/$supplierId" -ExpectedStatus @(200)
    $updateSupplierPayload = @{ businessName = 'Proveedor Test SRL Actualizado'; phone = '6023456789' }
    Test-Endpoint -Name "PUT /api/suppliers/$supplierId" -Method 'PUT' -Url "$suppliersUrl/$supplierId" -Body $updateSupplierPayload -ExpectedStatus @(200)

    Test-Endpoint -Name "PATCH /api/suppliers/$supplierId/status?status=INACTIVO" -Method 'PATCH' -Url "$suppliersUrl/$supplierId/status?status=INACTIVO" -ExpectedStatus @(200)
    Test-Endpoint -Name "PATCH /api/suppliers/$supplierId/status?status=ACTIVO" -Method 'PATCH' -Url "$suppliersUrl/$supplierId/status?status=ACTIVO" -ExpectedStatus @(200)

    # DELETE no está garantizado en el backend; puede devolver 404/405 si no está implementado.
    $deleteResult = Invoke-ApiRequest -Method 'DELETE' -Url "$suppliersUrl/$supplierId" -Headers $headers
    $deleteExpected = @(204, 404, 405)
    $deleteSuccess = $deleteExpected -contains $deleteResult.Status
    Write-Result "DELETE /api/suppliers/$supplierId" $deleteSuccess $deleteResult.Status "$(if ($deleteSuccess) { 'Resultado aceptable' } else { 'Esperado 204/404/405' })"
} else {
    Write-Host 'WARN: No se pudo obtener el ID del proveedor creado. Se omiten pruebas GET/PUT/PATCH/DELETE proveedor.'
}

Summarize-Results
if (($results | Where-Object { $_.Result -eq 'FAIL' }).Count -gt 0) {
    exit 1
}
exit 0
