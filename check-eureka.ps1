$eurekUrl = "http://localhost:8761/eureka/apps"

Write-Host "=== Verificando servicios registrados en Eureka ===" 

try {
    $response = Invoke-WebRequest -Uri $eurekUrl -WarningAction SilentlyContinue -UseBasicParsing
    Write-Host "OK - Eureka accesible"
    
    # Parse XML
    [xml]$xml = $response.Content
    $apps = $xml.applications.application
    
    Write-Host ""
    Write-Host "Servicios registrados:"
    foreach ($app in $apps) {
        Write-Host "  - $($app.name)"
        foreach ($instance in $app.instance) {
            Write-Host "    * $($instance.hostName):$($instance.port.InnerText) (Status: $($instance.status))"
        }
    }
} catch {
    Write-Host "ERROR - No se pudo acceder a Eureka: $($_.Exception.Message)"
}
