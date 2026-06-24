function Test-Endpoint {
    param($Name, $Method, $Uri, $Headers = @{}, $Body = $null, $ExpectedStatus = 200, $ContentType = "application/json")
    Write-Host "Running Test: $Name..." -NoNewline
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            Headers = $Headers
        }
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = $ContentType
        }
        
        $response = Invoke-WebRequest @params -ErrorAction Stop
        if ([int]$response.StatusCode -eq $ExpectedStatus) {
            Write-Host " [PASS] (Status: $($response.StatusCode))" -ForegroundColor Green
            return $response
        } else {
            Write-Host " [FAIL] (Expected $ExpectedStatus, got $($response.StatusCode))" -ForegroundColor Red
            return $null
        }
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            if ($statusCode -eq $ExpectedStatus) {
                 Write-Host " [PASS] (Status: $statusCode)" -ForegroundColor Green
                 return $_.Exception.Response
            }
            Write-Host " [FAIL] (Expected $ExpectedStatus, got $statusCode)" -ForegroundColor Red
        } else {
            Write-Host " [FAIL] (Error: $($_.Exception.Message))" -ForegroundColor Red
        }
        return $null
    }
}

$results = @()

# 1. Health Check
$res1 = Test-Endpoint "Health Check" "GET" "http://localhost:8084/actuator/health"
$results += [PSCustomObject]@{Test="Health Check"; Result=if($res1){"PASS"}else{"FAIL"}}

# 2. Legacy Login
$loginBody = @{ username="admin"; password="Admin@123" } | ConvertTo-Json
$res2 = Test-Endpoint "Legacy Login" "POST" "http://localhost:8084/api/auth/login" -Body $loginBody
$legacyToken = ""
if ($res2) {
    if ($res2.Content) {
        $content = $res2.Content | ConvertFrom-Json
        $legacyToken = $content.token
        $results += [PSCustomObject]@{Test="Legacy Login"; Result="PASS"}
    } else {
        $results += [PSCustomObject]@{Test="Legacy Login"; Result="FAIL (No content)"}
    }
} else {
    $results += [PSCustomObject]@{Test="Legacy Login"; Result="FAIL"}
}

# 3. Use Legacy Token
if ($legacyToken) {
    $headers = @{ Authorization = "Bearer $legacyToken" }
    $res3 = Test-Endpoint "Use Legacy Token" "GET" "http://localhost:8084/api/auth/users" -Headers $headers
    $results += [PSCustomObject]@{Test="Use Legacy Token"; Result=if($res3){"PASS"}else{"FAIL"}}
} else {
    $results += [PSCustomObject]@{Test="Use Legacy Token"; Result="SKIPPED (No Token)"}
}

# 4. OAuth2 Client Credentials
$oauthBody = "grant_type=client_credentials&client_id=microservices-client&client_secret=microservices-secret-change-me"
$res4 = Test-Endpoint "OAuth2 Token Request" "POST" "http://localhost:8084/oauth2/token" -Body $oauthBody -ContentType "application/x-www-form-urlencoded"
$oauthToken = ""
$refreshToken = ""
if ($res4) {
    try {
        $content = $res4.Content | ConvertFrom-Json
        $oauthToken = $content.access_token
        $refreshToken = $content.refresh_token
        $results += [PSCustomObject]@{Test="OAuth2 Client Credentials"; Result="PASS"}
    } catch {
        $results += [PSCustomObject]@{Test="OAuth2 Client Credentials"; Result="FAIL (JSON Parse error)"}
    }
} else {
    $results += [PSCustomObject]@{Test="OAuth2 Client Credentials"; Result="FAIL"}
}

# 5. Use OAuth Token
if ($oauthToken) {
    $headers = @{ Authorization = "Bearer $oauthToken" }
    $res5 = Test-Endpoint "Use OAuth Token" "GET" "http://localhost:8084/api/auth/users" -Headers $headers
    $results += [PSCustomObject]@{Test="Use OAuth Token"; Result=if($res5){"PASS"}else{"FAIL"}}
} else {
    $results += [PSCustomObject]@{Test="Use OAuth Token"; Result="SKIPPED (No Token)"}
}

# 6. Refresh Token
if ($refreshToken) {
    $refreshBody = "grant_type=refresh_token&refresh_token=$refreshToken&client_id=microservices-client&client_secret=microservices-secret-change-me"
    $res6 = Test-Endpoint "Refresh Token" "POST" "http://localhost:8084/oauth2/token" -Body $refreshBody -ContentType "application/x-www-form-urlencoded"
    $results += [PSCustomObject]@{Test="Refresh Token"; Result=if($res6){"PASS"}else{"FAIL"}}
} else {
    $results += [PSCustomObject]@{Test="Refresh Token"; Result="SKIPPED (No Refresh Token)"}
}

# 7. Token Revocation
if ($oauthToken) {
    $revokeBody = "token=$oauthToken&client_id=microservices-client&client_secret=microservices-secret-change-me"
    $res7a = Test-Endpoint "Revoke Token" "POST" "http://localhost:8084/oauth2/revoke" -Body $revokeBody -ContentType "application/x-www-form-urlencoded"
    $res7b = Test-Endpoint "Verify Revoked Token" "GET" "http://localhost:8084/api/auth/users" -Headers @{ Authorization = "Bearer $oauthToken" } -ExpectedStatus 401
    $results += [PSCustomObject]@{Test="Token Revocation"; Result=if($res7a -and $res7b){"PASS"}else{"FAIL"}}
} else {
    $results += [PSCustomObject]@{Test="Token Revocation"; Result="SKIPPED"}
}

# 8. Dual Validation (Check legacy token again)
if ($legacyToken) {
    $res8 = Test-Endpoint "Dual Validation (Legacy)" "GET" "http://localhost:8084/api/auth/users" -Headers @{ Authorization = "Bearer $legacyToken" }
    $results += [PSCustomObject]@{Test="Dual Validation"; Result=if($res8){"PASS"}else{"FAIL"}}
} else {
    $results += [PSCustomObject]@{Test="Dual Validation"; Result="SKIPPED"}
}

# 9. Gateway OAuth Routing
$res9 = Test-Endpoint "Gateway OAuth Routing" "POST" "http://localhost:8081/oauth2/token" -Body "grant_type=client_credentials&client_id=microservices-client&client_secret=microservices-secret-change-me" -ContentType "application/x-www-form-urlencoded"
$gatewayToken = ""
if ($res9) {
    $gatewayToken = ($res9.Content | ConvertFrom-Json).access_token
    $results += [PSCustomObject]@{Test="Gateway OAuth Routing"; Result="PASS"}
} else {
    $results += [PSCustomObject]@{Test="Gateway OAuth Routing"; Result="FAIL"}
}

# 10. Gateway Protected Endpoint
if ($gatewayToken) {
    $res10 = Test-Endpoint "Gateway Protected Endpoint" "GET" "http://localhost:8081/api/auth/users" -Headers @{ Authorization = "Bearer $gatewayToken" }
    $results += [PSCustomObject]@{Test="Gateway Protected Endpoint"; Result=if($res10){"PASS"}else{"FAIL"}}
} else {
    $results += [PSCustomObject]@{Test="Gateway Protected Endpoint"; Result="SKIPPED"}
}

Write-Host "`n--- Summary ---"
$results | Format-Table -AutoSize
