# Test login endpoints

Write-Host "`n=== Testing login on usuario-service (port 8084) ===" -ForegroundColor Cyan

try {
    $body = '{"username":"admin","password":"Admin@123"}'
    $response = Invoke-WebRequest -Uri 'http://localhost:8084/api/auth/login' `
        -Method Post `
        -Body $body `
        -ContentType 'application/json' `
        -WarningAction SilentlyContinue
    Write-Host "✓ SUCCESS (8084)" -ForegroundColor Green
    Write-Host "Status: $($response.StatusCode)"
    Write-Host "Response body:"
    $response.Content | ConvertFrom-Json | ConvertTo-Json | Write-Host
} catch {
    Write-Host "✗ FAILED (8084)" -ForegroundColor Red
    Write-Host "Status: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Error: $($_.Exception.Message)"
}

Write-Host "`n=== Testing login on API Gateway (port 8081) ===" -ForegroundColor Cyan

try {
    $body = '{"username":"admin","password":"Admin@123"}'
    $response = Invoke-WebRequest -Uri 'http://localhost:8081/api/auth/login' `
        -Method Post `
        -Body $body `
        -ContentType 'application/json' `
        -WarningAction SilentlyContinue
    Write-Host "✓ SUCCESS (8081)" -ForegroundColor Green
    Write-Host "Status: $($response.StatusCode)"
    Write-Host "Response body:"
    $response.Content | ConvertFrom-Json | ConvertTo-Json | Write-Host
} catch {
    Write-Host "✗ FAILED (8081)" -ForegroundColor Red
    Write-Host "Status: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Error: $($_.Exception.Message)"
}
