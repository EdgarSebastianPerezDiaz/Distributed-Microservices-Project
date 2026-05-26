
# =============================================
# SUITE DE PRUEBAS DE INTEGRACIÓN OAUTH2
# =============================================

Write-Output "`n================ INICIANDO PRUEBAS ================"
Write-Output "Marca de tiempo: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Output "================================================`n"

# Colores para salida
$passColor = 'Green'
$failColor = 'Red'
$infoColor = 'Cyan'

# Resultados
$results = @()

# =============================================
# TEST 1: Health Check (usuario-service)
# =============================================
Write-Output "[TEST 1] Health Check usuario-service (8084)"
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8084/actuator/health" -ErrorAction Stop
    $status = $response.StatusCode
    Write-Host "✓ PASS - Status: $status" -ForegroundColor $passColor
    $results += "TEST 1 - Health Check (8084): PASS ($status)"
} catch {
    Write-Host "✗ FAIL - Error: $_" -ForegroundColor $failColor
    $results += "TEST 1 - Health Check (8084): FAIL ($_)"
}

# =============================================
# TEST 2: Legacy JWT Login
# =============================================
Write-Output "`n[TEST 2] Legacy JWT Login"
try {
    $loginBody = @{username="admin";password="Admin@123"} | ConvertTo-Json
    $response = Invoke-WebRequest -Uri "http://localhost:8084/api/auth/login" `
        -Method Post `
        -Body $loginBody `
        -ContentType "application/json" `
        -ErrorAction Stop
    $responseObj = $response.Content | ConvertFrom-Json
    $token = $responseObj.token
    Write-Host "✓ PASS - Status: $($response.StatusCode), Token: $($token.Substring(0,30))..." -ForegroundColor $passColor
    $results += "TEST 2 - Legacy JWT Login: PASS"
    
    # Guardar token para tests posteriores
    $script:legacyToken = $token
} catch {
    Write-Host "✗ FAIL - Error: $_" -ForegroundColor $failColor
    $results += "TEST 2 - Legacy JWT Login: FAIL"
    $script:legacyToken = $null
}

# =============================================
# TEST 3: Use Legacy Token
# =============================================
Write-Output "`n[TEST 3] Use Legacy JWT Token (GET /api/auth/users)"
if ($script:legacyToken) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8084/api/auth/users" `
            -Headers @{Authorization="Bearer $($script:legacyToken)"} `
            -ErrorAction Stop
        Write-Host "✓ PASS - Status: $($response.StatusCode)" -ForegroundColor $passColor
        $results += "TEST 3 - Use Legacy Token: PASS"
    } catch {
        Write-Host "✗ FAIL - Error: $_" -ForegroundColor $failColor
        $results += "TEST 3 - Use Legacy Token: FAIL"
    }
} else {
    Write-Host "⊘ SKIP - No legacy token available" -ForegroundColor $infoColor
    $results += "TEST 3 - Use Legacy Token: SKIP"
}

# =============================================
# TEST 4: OAuth2 Client Credentials
# =============================================
Write-Output "`n[TEST 4] OAuth2 Client Credentials Token"
try {
    $oauth2Body = @{
        grant_type="client_credentials"
        client_id="microservices-client"
        client_secret="microservices-secret-change-me"
    } | ConvertTo-Json
    $response = Invoke-WebRequest -Uri "http://localhost:8084/oauth2/token" `
        -Method Post `
        -Body $oauth2Body `
        -ContentType "application/json" `
        -ErrorAction Stop
    $responseObj = $response.Content | ConvertFrom-Json
    $oauthToken = $responseObj.access_token
    Write-Host "✓ PASS - Status: $($response.StatusCode), OAuth Token: $($oauthToken.Substring(0,30))..." -ForegroundColor $passColor
    $results += "TEST 4 - OAuth2 Client Credentials: PASS"
    
    # Guardar token para tests posteriores
    $script:oauthToken = $oauthToken
} catch {
    Write-Host "✗ FAIL - Error: $_" -ForegroundColor $failColor
    $results += "TEST 4 - OAuth2 Client Credentials: FAIL"
    $script:oauthToken = $null
}

# =============================================
# TEST 5: Use OAuth Token
# =============================================
Write-Output "`n[TEST 5] Use OAuth Token (GET /api/auth/users)"
if ($script:oauthToken) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8084/api/auth/users" `
            -Headers @{Authorization="Bearer $($script:oauthToken)"} `
            -ErrorAction Stop
        Write-Host "✓ PASS - Status: $($response.StatusCode)" -ForegroundColor $passColor
        $results += "TEST 5 - Use OAuth Token: PASS"
    } catch {
        Write-Host "✗ FAIL - Error: $_" -ForegroundColor $failColor
        $results += "TEST 5 - Use OAuth Token: FAIL"
    }
} else {
    Write-Host "⊘ SKIP - No OAuth token available" -ForegroundColor $infoColor
    $results += "TEST 5 - Use OAuth Token: SKIP"
}

# =============================================
# TEST 6: Dual Validation (Legacy Token Still Works)
# =============================================
Write-Output "`n[TEST 6] Dual Validation (Legacy Token After OAuth2 Tests)"
if ($script:legacyToken) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8084/api/auth/users" `
            -Headers @{Authorization="Bearer $($script:legacyToken)"} `
            -ErrorAction Stop
        Write-Host "✓ PASS - Legacy token still valid - Status: $($response.StatusCode)" -ForegroundColor $passColor
        $results += "TEST 6 - Dual Validation: PASS"
    } catch {
        Write-Host "✗ FAIL - Legacy token no longer works" -ForegroundColor $failColor
        $results += "TEST 6 - Dual Validation: FAIL"
    }
} else {
    Write-Host "⊘ SKIP - No legacy token available" -ForegroundColor $infoColor
    $results += "TEST 6 - Dual Validation: SKIP"
}

# =============================================
# TEST 7: Gateway Health Check
# =============================================
Write-Output "`n[TEST 7] Gateway Health Check (8081)"
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" `
        -ErrorAction Stop
    Write-Host "✓ PASS - Status: $($response.StatusCode)" -ForegroundColor $passColor
    $results += "TEST 7 - Gateway Health: PASS"
} catch {
    Write-Host "✗ FAIL - Error: $_" -ForegroundColor $failColor
    $results += "TEST 7 - Gateway Health: FAIL"
}

# =============================================
# TEST 8: Gateway OAuth2 Routing
# =============================================
Write-Output "`n[TEST 8] Gateway OAuth2 Token (via gateway 8081)"
try {
    $oauth2Body = @{
        grant_type="client_credentials"
        client_id="microservices-client"
        client_secret="microservices-secret-change-me"
    } | ConvertTo-Json
    $response = Invoke-WebRequest -Uri "http://localhost:8081/oauth2/token" `
        -Method Post `
        -Body $oauth2Body `
        -ContentType "application/json" `
        -ErrorAction Stop
    Write-Host "✓ PASS - Status: $($response.StatusCode)" -ForegroundColor $passColor
    $results += "TEST 8 - Gateway OAuth Routing: PASS"
} catch {
    Write-Host "✗ FAIL - Error: $_" -ForegroundColor $failColor
    $results += "TEST 8 - Gateway OAuth Routing: FAIL"
}

# =============================================
# TEST 9: Gateway Protected Endpoint
# =============================================
Write-Output "`n[TEST 9] Gateway Protected Endpoint (via gateway 8081)"
if ($script:oauthToken) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/users" `
            -Headers @{Authorization="Bearer $($script:oauthToken)"} `
            -ErrorAction Stop
        Write-Host "✓ PASS - Status: $($response.StatusCode)" -ForegroundColor $passColor
        $results += "TEST 9 - Gateway Protected Endpoint: PASS"
    } catch {
        Write-Host "✗ FAIL - Error: $_" -ForegroundColor $failColor
        $results += "TEST 9 - Gateway Protected Endpoint: FAIL"
    }
} else {
    Write-Host "⊘ SKIP - No OAuth token available" -ForegroundColor $infoColor
    $results += "TEST 9 - Gateway Protected Endpoint: SKIP"
}

# =============================================
# TEST 10: JWKS Endpoint (Public Key for Verification)
# =============================================
Write-Output "`n[TEST 10] JWKS Endpoint (/.well-known/jwks.json)"
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8084/.well-known/jwks.json" `
        -ErrorAction Stop
    Write-Host "✓ PASS - Status: $($response.StatusCode)" -ForegroundColor $passColor
    $results += "TEST 10 - JWKS Endpoint: PASS"
} catch {
    Write-Host "✗ FAIL - Error: $_" -ForegroundColor $failColor
    $results += "TEST 10 - JWKS Endpoint: FAIL"
}

# =============================================
# RESUMEN DE RESULTADOS
# =============================================
Write-Output "`n`n================ RESUMEN DE PRUEBAS ================"
$passCount = ($results | Where-Object { $_ -match "PASS" }).Count
$failCount = ($results | Where-Object { $_ -match "FAIL" }).Count
$skipCount = ($results | Where-Object { $_ -match "SKIP" }).Count

foreach ($result in $results) {
    if ($result -match "PASS") {
        Write-Host "✓ $result" -ForegroundColor $passColor
    } elseif ($result -match "FAIL") {
        Write-Host "✗ $result" -ForegroundColor $failColor
    } else {
        Write-Host "⊘ $result" -ForegroundColor $infoColor
    }
}

Write-Output "`n================================================"
Write-Output "Total: $($results.Count) | PASS: $passCount | FAIL: $failCount | SKIP: $skipCount"
Write-Output "================================================`n"

# Determinar resultado final
if ($failCount -eq 0 -and $passCount -gt 0) {
    Write-Host "🎉 TODAS LAS PRUEBAS PASARON CORRECTAMENTE" -ForegroundColor $passColor
    exit 0
} else {
    Write-Host "❌ ALGUNAS PRUEBAS FALLARON" -ForegroundColor $failColor
    exit 1
}
