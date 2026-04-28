#!/usr/bin/env pwsh

# Script rápido para crear usuario auditor
$token = (Invoke-RestMethod -Uri "http://localhost:8084/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}').token

$auditorData = @{
    username = "auditor"
    email = "auditor@uptc.edu.co"
    password = "auditor123"
    fullName = "Usuario Auditor"
    role = "AUDITOR"
} | ConvertTo-Json

$result = Invoke-RestMethod -Uri "http://localhost:8084/api/auth/register" -Method Post -ContentType "application/json" -Headers @{"Authorization"="Bearer $token"} -Body $auditorData

Write-Host "Usuario Auditor creado:"
$result | ConvertTo-Json
