@echo off
REM Test login endpoint directly
echo Testing login on usuario-service (port 8084):
powershell -Command "
try {
    \$body = '{\"username\":\"admin\",\"password\":\"Admin@123\"}'
    \$response = Invoke-WebRequest -Uri 'http://localhost:8084/api/auth/login' `
        -Method Post `
        -Body \$body `
        -ContentType 'application/json' `
        -ErrorAction Stop
    Write-Host 'SUCCESS (8084):'
    Write-Host \$response.StatusCode
    Write-Host \$response.Content
} catch {
    Write-Host 'ERROR (8084):'
    Write-Host \$_.Exception.Response.StatusCode
}
"

echo.
echo Testing login on API Gateway (port 8081):
powershell -Command "
try {
    \$body = '{\"username\":\"admin\",\"password\":\"Admin@123\"}'
    \$response = Invoke-WebRequest -Uri 'http://localhost:8081/api/auth/login' `
        -Method Post `
        -Body \$body `
        -ContentType 'application/json' `
        -ErrorAction Stop
    Write-Host 'SUCCESS (8081):'
    Write-Host \$response.StatusCode
    Write-Host \$response.Content
} catch {
    Write-Host 'ERROR (8081):'
    Write-Host \$_.Exception.Response.StatusCode
}
"
