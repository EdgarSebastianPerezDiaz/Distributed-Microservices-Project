# ============================================================================
# SCRIPT DE PRUEBAS DE INTEGRACIÓN - OAuth 2.0 DUAL AUTHENTICATION
# ============================================================================
# Verifica que el sistema soporta tanto JWT Legacy (HS512) como OAuth2 (RS256)

$ErrorActionPreference = "Stop"

# Colores para output
$GREEN = "`e[32m"
$RED = "`e[31m"
$YELLOW = "`e[33m"
$RESET = "`e[0m"

# Variables globales
$USUARIO_SERVICE = "http://localhost:8084"
$API_GATEWAY = "http://localhost:8081"
$legacy_token = $null
$oauth_access_token = $null
$oauth_refresh_token = $null
$test_count = 0
$pass_count = 0
$fail_count = 0

# Funciones helper
function Write-Test {
    param([string]$name)
    $script:test_count = $script:test_count + 1
    Write-Host "`n$YELLOW[TEST $($script:test_count)]$RESET $name"
}

function Write-Pass {
    param([string]$msg)
    $script:pass_count = $script:pass_count + 1
    Write-Host "$GREEN`u{2713} PASS:$RESET $msg"
}

function Write-Fail {
    param([string]$msg)
    $script:fail_count = $script:fail_count + 1
    Write-Host "$RED`u{2717} FAIL:$RESET $msg"
}

function Test-Response {
    param(
        [object]$response,
        [int]$expectedCode,
        [string]$testName
    )
    
    if ($response.StatusCode -eq $expectedCode) {
        Write-Pass "$testName (HTTP $($response.StatusCode))"
        return $response.Content | ConvertFrom-Json
    } else {
        Write-Fail "$testName - Expected $expectedCode, got $($response.StatusCode)"
        Write-Host "Response: $($response.Content)"
        throw "Test failed: $testName"
    }
}

# ============================================================================
# TEST 1: Health Checks
# ============================================================================
Write-Test "Health Check - Usuario Service"
try {
    $response = Invoke-WebRequest -Uri "$USUARIO_SERVICE/actuator/health" -Method Get -ErrorAction Stop
    Test-Response $response 200 "Usuario Service Health" | Out-Null
} catch {
    Write-Fail "Usuario Service Health - $_"
    exit 1
}

Write-Test "Health Check - API Gateway"
try {
    $response = Invoke-WebRequest -Uri "$API_GATEWAY/actuator/health" -Method Get -ErrorAction Stop
    Test-Response $response 200 "API Gateway Health" | Out-Null
} catch {
    Write-Fail "API Gateway Health - $_"
    exit 1
}

# ============================================================================
# TEST 2: Login Legacy JWT (HS512)
# ============================================================================
Write-Test "Login Legacy JWT - Obtener token HS512"
try {
    $loginData = @{
        username = "admin"
        password = "Admin@123"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "$USUARIO_SERVICE/api/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginData `
        -ErrorAction Stop
    
    $result = Test-Response $response 200 "Login Legacy JWT"
    
    if ($result.token) {
        $script:legacy_token = $result.token
        Write-Host "  Token Legacy: $($script:legacy_token.Substring(0, 30))..."
    } else {
        Write-Fail "Login Legacy - Token vacío en respuesta"
        exit 1
    }
} catch {
    Write-Fail "Login Legacy JWT - $_"
    exit 1
}

# ============================================================================
# TEST 3: Usar token Legacy en endpoint protegido
# ============================================================================
Write-Test "Usar token Legacy - GET /api/auth/users"
try {
    $headers = @{
        "Authorization" = "Bearer $script:legacy_token"
    }
    
    $response = Invoke-WebRequest -Uri "$USUARIO_SERVICE/api/auth/users" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop
    
    $result = Test-Response $response 200 "Token Legacy en endpoint protegido"
    Write-Host "  Usuarios retornados: $($result.Count)"
} catch {
    Write-Fail "Usar token Legacy - $_"
    exit 1
}

# ============================================================================
# TEST 4: OAuth 2.0 Client Credentials
# ============================================================================
Write-Test "OAuth 2.0 - Client Credentials Flow"
try {
    $body = @{
        grant_type = "client_credentials"
        client_id = "microservices-client"
        client_secret = "microservices-secret-change-me"
        scope = "read"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "$USUARIO_SERVICE/oauth2/token" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body `
        -ErrorAction Stop
    
    $result = Test-Response $response 200 "OAuth2 Client Credentials"
    
    if ($result.access_token -and $result.refresh_token) {
        $script:oauth_access_token = $result.access_token
        $script:oauth_refresh_token = $result.refresh_token
        Write-Host "  Access Token: $($script:oauth_access_token.Substring(0, 30))..."
        Write-Host "  Refresh Token: $($script:oauth_refresh_token.Substring(0, 30))..."
        Write-Host "  Expires In: $($result.expires_in) segundos"
    } else {
        Write-Fail "OAuth2 - Access token o refresh token vacío"
        exit 1
    }
} catch {
    Write-Fail "OAuth 2.0 Client Credentials - $_"
    exit 1
}

# ============================================================================
# TEST 5: Usar token OAuth en endpoint protegido
# ============================================================================
Write-Test "Usar token OAuth - GET /api/auth/users"
try {
    $headers = @{
        "Authorization" = "Bearer $script:oauth_access_token"
    }
    
    $response = Invoke-WebRequest -Uri "$USUARIO_SERVICE/api/auth/users" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop
    
    $result = Test-Response $response 200 "Token OAuth en endpoint protegido"
    Write-Host "  Usuarios retornados: $($result.Count)"
} catch {
    Write-Fail "Usar token OAuth - $_"
    exit 1
}

# ============================================================================
# TEST 6: Refresh Token
# ============================================================================
Write-Test "OAuth 2.0 - Refresh Token"
try {
    $body = @{
        grant_type = "refresh_token"
        refresh_token = $script:oauth_refresh_token
        client_id = "microservices-client"
        client_secret = "microservices-secret-change-me"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "$USUARIO_SERVICE/oauth2/token" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body `
        -ErrorAction Stop
    
    $result = Test-Response $response 200 "Refresh Token"
    
    if ($result.access_token) {
        $script:oauth_access_token = $result.access_token
        Write-Host "  Nuevo Access Token: $($script:oauth_access_token.Substring(0, 30))..."
    } else {
        Write-Fail "Refresh Token - Access token vacío"
        exit 1
    }
} catch {
    Write-Fail "Refresh Token - $_"
    exit 1
}

# ============================================================================
# TEST 7: Token Revocation
# ============================================================================
Write-Test "OAuth 2.0 - Token Revocation"
try {
    $body = @{
        token = $script:oauth_refresh_token
        client_id = "microservices-client"
        client_secret = "microservices-secret-change-me"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "$USUARIO_SERVICE/oauth2/revoke" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body `
        -ErrorAction Stop
    
    Test-Response $response 200 "Token Revocation" | Out-Null
} catch {
    Write-Fail "Token Revocation - $_"
    exit 1
}

# Verificar que el token revocado NO funciona
Write-Test "Verificar token revocado es rechazado"
try {
    $headers = @{
        "Authorization" = "Bearer $script:oauth_refresh_token"
    }
    
    try {
        $response = Invoke-WebRequest -Uri "$USUARIO_SERVICE/api/auth/users" `
            -Method Get `
            -Headers $headers `
            -ErrorAction Stop
        Write-Fail "Token revocado debería retornar 401"
        exit 1
    } catch {
        if ($_.Exception.Response.StatusCode -eq 401 -or $_.Exception.Response.StatusCode -eq 403) {
            Write-Pass "Token revocado correctamente rechazado (401)"
        } else {
            Write-Fail "Token revocado - Status code inesperado: $($_.Exception.Response.StatusCode)"
            exit 1
        }
    }
} catch {
    Write-Fail "Verificar token revocado - $_"
    exit 1
}

# ============================================================================
# TEST 8: Dual Validation - Token Legacy sigue siendo válido
# ============================================================================
Write-Test "Validación Dual - Token Legacy aún válido"
try {
    $headers = @{
        "Authorization" = "Bearer $script:legacy_token"
    }
    
    $response = Invoke-WebRequest -Uri "$USUARIO_SERVICE/api/auth/users" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop
    
    Test-Response $response 200 "Token Legacy sigue siendo válido" | Out-Null
} catch {
    Write-Fail "Dual Validation - Token Legacy - $_"
    exit 1
}

# ============================================================================
# TEST 9: API Gateway Routing - OAuth endpoints
# ============================================================================
Write-Test "API Gateway Routing - POST /oauth2/token"
try {
    $body = @{
        grant_type = "client_credentials"
        client_id = "microservices-client"
        client_secret = "microservices-secret-change-me"
        scope = "read"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "$API_GATEWAY/oauth2/token" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body `
        -ErrorAction Stop
    
    $result = Test-Response $response 200 "API Gateway OAuth2 Token routing"
    Write-Host "  Token obtenido desde gateway: $($result.access_token.Substring(0, 30))..."
} catch {
    Write-Fail "API Gateway OAuth2 routing - $_"
    exit 1
}

# ============================================================================
# TEST 10: API Gateway Routing - Endpoint protegido con token del gateway
# ============================================================================
Write-Test "API Gateway Routing - GET /api/auth/users a través del gateway"
try {
    # Primero obtenemos un token del gateway
    $tokenBody = @{
        grant_type = "client_credentials"
        client_id = "microservices-client"
        client_secret = "microservices-secret-change-me"
    } | ConvertTo-Json
    
    $tokenResponse = Invoke-WebRequest -Uri "$API_GATEWAY/oauth2/token" `
        -Method Post `
        -ContentType "application/json" `
        -Body $tokenBody `
        -ErrorAction Stop
    
    $tokenResult = $tokenResponse.Content | ConvertFrom-Json
    $gatewayToken = $tokenResult.access_token
    
    # Luego usamos ese token para acceder a un endpoint protegido
    $headers = @{
        "Authorization" = "Bearer $gatewayToken"
    }
    
    $response = Invoke-WebRequest -Uri "$API_GATEWAY/api/auth/users" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop
    
    Test-Response $response 200 "API Gateway protected endpoint routing" | Out-Null
} catch {
    Write-Fail "API Gateway protected endpoint routing - $_"
    exit 1
}

# ============================================================================
# RESUMEN FINAL
# ============================================================================
Write-Host "`n"
Write-Host "════════════════════════════════════════════════════════════════"
Write-Host "RESUMEN DE PRUEBAS"
Write-Host "════════════════════════════════════════════════════════════════"

Write-Host "Total de pruebas: $test_count"
Write-Host "$GREEN✓ Pasadas: $pass_count$RESET"
Write-Host "$RED✗ Fallidas: $fail_count$RESET"

if ($fail_count -eq 0) {
    Write-Host "`n$GREEN════════════════════════════════════════════════════════════════$RESET"
    Write-Host "$GREEN`u{2713}TODAS LAS PRUEBAS PASARON EXITOSAMENTE!$RESET"
    Write-Host "$GREEN════════════════════════════════════════════════════════════════$RESET"
    exit 0
} else {
    Write-Host "`n$RED════════════════════════════════════════════════════════════════$RESET"
    Write-Host "$RED`u{2717}ALGUNAS PRUEBAS FALLARON!$RESET"
    Write-Host "$RED════════════════════════════════════════════════════════════════$RESET"
    exit 1
}
