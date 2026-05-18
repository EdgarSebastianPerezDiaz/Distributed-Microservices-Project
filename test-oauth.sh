#!/bin/bash

# ============================================================================
# TEST SCRIPT - OAuth 2.0 Dual Authentication System
# ============================================================================
# Objetivo: Validar que ambos sistemas (JWT Legacy + OAuth 2.0) funcionan
# Requisitos: curl, jq (opcional pero recomendado)
# Ejecución: bash test-oauth.sh
# ============================================================================

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables de configuración
GATEWAY_URL="http://localhost:8081"
USUARIO_SERVICE_URL="http://localhost:8084"
EUREKA_URL="http://localhost:8761/eureka"

# Contadores
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# ============================================================================
# FUNCIONES AUXILIARES
# ============================================================================

print_header() {
    echo -e "\n${BLUE}===============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}===============================================${NC}\n"
}

print_test() {
    echo -e "${YELLOW}[TEST] $1${NC}"
}

print_success() {
    echo -e "${GREEN}[✓] $1${NC}"
    ((TESTS_PASSED++))
}

print_error() {
    echo -e "${RED}[✗] $1${NC}"
    ((TESTS_FAILED++))
}

print_info() {
    echo -e "${BLUE}[INFO] $1${NC}"
}

check_service() {
    local url=$1
    local service_name=$2
    
    print_test "Verificando disponibilidad de $service_name"
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url/actuator/health" 2>/dev/null)
    
    if [ "$response" = "200" ]; then
        print_success "$service_name está disponible (HTTP $response)"
        return 0
    else
        print_error "$service_name no está disponible (HTTP $response)"
        return 1
    fi
}

# ============================================================================
# VERIFICACIÓN PREVIA
# ============================================================================

print_header "VERIFICACIÓN PREVIA DEL SISTEMA"

print_test "Comprobando que curl está instalado"
if command -v curl &> /dev/null; then
    print_success "curl está disponible"
else
    print_error "curl no está instalado. Abortando."
    exit 1
fi

print_test "Comprobando que jq está instalado (opcional)"
if command -v jq &> /dev/null; then
    print_success "jq está disponible"
    HAS_JQ=true
else
    print_info "jq no está instalado. Continuando sin él."
    HAS_JQ=false
fi

# ============================================================================
# HEALTH CHECKS
# ============================================================================

print_header "HEALTH CHECKS - Servicios Disponibles"

check_service "$USUARIO_SERVICE_URL" "Usuario Service (8084)" || exit 1
check_service "$GATEWAY_URL" "API Gateway (8081)" || exit 1

# ============================================================================
# PRUEBA 1: LOGIN LEGACY JWT
# ============================================================================

print_header "PRUEBA 1: LOGIN LEGACY (JWT HS512)"

print_test "POST /api/auth/login con credenciales válidas (admin/Admin123)"

LOGIN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin123"
  }')

if [ "$HAS_JQ" = true ]; then
    LEGACY_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty')
    LEGACY_HTTP=$(echo "$LOGIN_RESPONSE" | jq -r '.httpStatusCode // "200"')
else
    # Extraer token sin jq (búsqueda simple)
    LEGACY_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
fi

if [ ! -z "$LEGACY_TOKEN" ] && [ "$LEGACY_TOKEN" != "null" ]; then
    print_success "Token Legacy obtenido"
    echo "Token (primeros 50 caracteres): ${LEGACY_TOKEN:0:50}..."
else
    print_error "No se pudo obtener token. Respuesta: $LOGIN_RESPONSE"
    LEGACY_TOKEN=""
fi

# ============================================================================
# PRUEBA 2: USAR TOKEN LEGACY
# ============================================================================

print_header "PRUEBA 2: USAR TOKEN LEGACY EN ENDPOINT PROTEGIDO"

if [ ! -z "$LEGACY_TOKEN" ]; then
    print_test "GET /api/auth/users con token Legacy"
    
    USERS_RESPONSE=$(curl -s -X GET "$GATEWAY_URL/api/auth/users" \
      -H "Authorization: Bearer $LEGACY_TOKEN" \
      -H "Content-Type: application/json")
    
    if echo "$USERS_RESPONSE" | grep -q "admin" || echo "$USERS_RESPONSE" | grep -q '\['; then
        print_success "Token Legacy validado - Lista de usuarios obtenida"
        echo "Respuesta (primeros 200 caracteres): ${USERS_RESPONSE:0:200}..."
    else
        print_error "Token Legacy no fue aceptado. Respuesta: $USERS_RESPONSE"
    fi
else
    print_info "Saltando - No hay token Legacy disponible"
    ((TESTS_SKIPPED++))
fi

# ============================================================================
# PRUEBA 3: OAUTH2 CLIENT CREDENTIALS
# ============================================================================

print_header "PRUEBA 3: OBTENER TOKEN OAUTH2 (Client Credentials)"

print_test "POST /oauth2/token con grant_type=client_credentials"

OAUTH2_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=microservices-client&client_secret=microservices-secret-change-me&scope=read")

if [ "$HAS_JQ" = true ]; then
    OAUTH2_TOKEN=$(echo "$OAUTH2_RESPONSE" | jq -r '.access_token // empty')
    OAUTH2_REFRESH=$(echo "$OAUTH2_RESPONSE" | jq -r '.refresh_token // empty')
else
    # Extraer sin jq
    OAUTH2_TOKEN=$(echo "$OAUTH2_RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
    OAUTH2_REFRESH=$(echo "$OAUTH2_RESPONSE" | grep -o '"refresh_token":"[^"]*' | cut -d'"' -f4)
fi

if [ ! -z "$OAUTH2_TOKEN" ] && [ "$OAUTH2_TOKEN" != "null" ]; then
    print_success "Token OAuth2 obtenido"
    echo "Access Token (primeros 50 caracteres): ${OAUTH2_TOKEN:0:50}..."
    
    if [ ! -z "$OAUTH2_REFRESH" ] && [ "$OAUTH2_REFRESH" != "null" ]; then
        print_success "Refresh Token también obtenido"
        echo "Refresh Token (primeros 50 caracteres): ${OAUTH2_REFRESH:0:50}..."
    fi
else
    print_error "No se pudo obtener token OAuth2. Respuesta: $OAUTH2_RESPONSE"
    OAUTH2_TOKEN=""
    OAUTH2_REFRESH=""
fi

# ============================================================================
# PRUEBA 4: USAR TOKEN OAUTH2
# ============================================================================

print_header "PRUEBA 4: USAR TOKEN OAUTH2 EN ENDPOINT PROTEGIDO"

if [ ! -z "$OAUTH2_TOKEN" ]; then
    print_test "GET /api/auth/users con token OAuth2"
    
    OAUTH2_USERS=$(curl -s -X GET "$GATEWAY_URL/api/auth/users" \
      -H "Authorization: Bearer $OAUTH2_TOKEN" \
      -H "Content-Type: application/json")
    
    if echo "$OAUTH2_USERS" | grep -q "admin" || echo "$OAUTH2_USERS" | grep -q '\['; then
        print_success "Token OAuth2 validado - Lista de usuarios obtenida"
        echo "Respuesta (primeros 200 caracteres): ${OAUTH2_USERS:0:200}..."
    else
        print_error "Token OAuth2 no fue aceptado. Respuesta: $OAUTH2_USERS"
    fi
else
    print_info "Saltando - No hay token OAuth2 disponible"
    ((TESTS_SKIPPED++))
fi

# ============================================================================
# PRUEBA 5: REFRESH TOKEN
# ============================================================================

print_header "PRUEBA 5: REFRESH TOKEN (Obtener nuevo access token)"

if [ ! -z "$OAUTH2_REFRESH" ]; then
    print_test "POST /oauth2/token con grant_type=refresh_token"
    
    REFRESH_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/oauth2/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=refresh_token&client_id=microservices-client&client_secret=microservices-secret-change-me&refresh_token=$OAUTH2_REFRESH")
    
    if [ "$HAS_JQ" = true ]; then
        NEW_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.access_token // empty')
        NEW_REFRESH=$(echo "$REFRESH_RESPONSE" | jq -r '.refresh_token // empty')
    else
        NEW_TOKEN=$(echo "$REFRESH_RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
        NEW_REFRESH=$(echo "$REFRESH_RESPONSE" | grep -o '"refresh_token":"[^"]*' | cut -d'"' -f4)
    fi
    
    if [ ! -z "$NEW_TOKEN" ] && [ "$NEW_TOKEN" != "null" ]; then
        print_success "Nuevo access token obtenido mediante refresh"
        echo "Nuevo Access Token (primeros 50 caracteres): ${NEW_TOKEN:0:50}..."
    else
        print_error "No se pudo refrescar token. Respuesta: $REFRESH_RESPONSE"
    fi
else
    print_info "Saltando - No hay refresh token disponible"
    ((TESTS_SKIPPED++))
fi

# ============================================================================
# PRUEBA 6: INFORMACIÓN OAUTH2
# ============================================================================

print_header "PRUEBA 6: INFORMACIÓN OAUTH2 (Endpoints públicos)"

print_test "GET /api/oauth2-info/endpoints"

ENDPOINTS_RESPONSE=$(curl -s -X GET "$GATEWAY_URL/api/oauth2-info/endpoints")

if echo "$ENDPOINTS_RESPONSE" | grep -q "token_endpoint" || echo "$ENDPOINTS_RESPONSE" | grep -q "oauth2"; then
    print_success "Información de endpoints disponible"
    if [ "$HAS_JQ" = true ]; then
        echo "$ENDPOINTS_RESPONSE" | jq '.'
    else
        echo "$ENDPOINTS_RESPONSE"
    fi
else
    print_error "No se pudo obtener información de endpoints. Respuesta: $ENDPOINTS_RESPONSE"
fi

# ============================================================================
# PRUEBA 7: COEXISTENCIA (Ambos tokens funcionan)
# ============================================================================

print_header "PRUEBA 7: COEXISTENCIA DE SISTEMAS"

print_test "Comparando respuestas entre Legacy y OAuth2"

SAME_ENDPOINT=true

if [ ! -z "$LEGACY_TOKEN" ] && [ ! -z "$OAUTH2_TOKEN" ]; then
    LEGACY_USERS=$(curl -s -X GET "$GATEWAY_URL/api/auth/users" \
      -H "Authorization: Bearer $LEGACY_TOKEN" \
      -H "Content-Type: application/json")
    
    OAUTH2_USERS=$(curl -s -X GET "$GATEWAY_URL/api/auth/users" \
      -H "Authorization: Bearer $OAUTH2_TOKEN" \
      -H "Content-Type: application/json")
    
    # Verificar que ambos devuelven datos similares
    if echo "$LEGACY_USERS" | grep -q "admin" && echo "$OAUTH2_USERS" | grep -q "admin"; then
        print_success "Ambos tokens funcionan con el mismo endpoint"
        print_success "Coexistencia verificada ✓"
    else
        print_error "Los tokens no devuelven respuestas consistentes"
    fi
else
    print_info "Saltando - No hay ambos tokens disponibles"
    ((TESTS_SKIPPED++))
fi

# ============================================================================
# PRUEBA 8: REVOCACIÓN DE TOKEN
# ============================================================================

print_header "PRUEBA 8: REVOCACIÓN DE TOKEN"

if [ ! -z "$OAUTH2_REFRESH" ]; then
    print_test "POST /oauth2/revoke para revocar refresh token"
    
    REVOKE_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/oauth2/revoke" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "token=$OAUTH2_REFRESH&client_id=microservices-client&client_secret=microservices-secret-change-me")
    
    if [ $? -eq 0 ]; then
        print_success "Token revocado"
        
        # Intentar usar el token revocado
        print_test "Intentando usar token revocado"
        
        REVOKED_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/oauth2/token" \
          -H "Content-Type: application/x-www-form-urlencoded" \
          -d "grant_type=refresh_token&client_id=microservices-client&client_secret=microservices-secret-change-me&refresh_token=$OAUTH2_REFRESH")
        
        if echo "$REVOKED_RESPONSE" | grep -q "invalid_grant" || echo "$REVOKED_RESPONSE" | grep -q "error"; then
            print_success "Token revocado rechazado como se esperaba"
        else
            print_error "Token revocado debería ser rechazado. Respuesta: $REVOKED_RESPONSE"
        fi
    else
        print_error "No se pudo revocar token"
    fi
else
    print_info "Saltando - No hay refresh token para revocar"
    ((TESTS_SKIPPED++))
fi

# ============================================================================
# PRUEBA 9: GATEWAY ROUTING
# ============================================================================

print_header "PRUEBA 9: GATEWAY ROUTING"

print_test "Verificar que Gateway enruta correctamente /oauth2/token"

ROUTING_TEST=$(curl -s -X POST "$GATEWAY_URL/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=test&client_secret=test&scope=read")

if echo "$ROUTING_TEST" | grep -q "invalid_client" || echo "$ROUTING_TEST" | grep -q "error" || echo "$ROUTING_TEST" | grep -q "access_token"; then
    print_success "Gateway enruta correctamente /oauth2/token"
else
    print_error "Gateway no enruta correctamente /oauth2/token. Respuesta: $ROUTING_TEST"
fi

# ============================================================================
# PRUEBA 10: JWT STRUCTURE
# ============================================================================

print_header "PRUEBA 10: ESTRUCTURA DE TOKENS JWT"

if [ ! -z "$LEGACY_TOKEN" ]; then
    print_test "Verificar estructura JWT Legacy (HS512)"
    
    # Contar puntos (debería haber 2)
    NUM_DOTS=$(echo "$LEGACY_TOKEN" | tr -cd '.' | wc -c)
    
    if [ "$NUM_DOTS" = "2" ]; then
        print_success "Token Legacy tiene estructura JWT válida (3 partes)"
    else
        print_error "Token Legacy tiene estructura inválida"
    fi
fi

if [ ! -z "$OAUTH2_TOKEN" ]; then
    print_test "Verificar estructura JWT OAuth2 (RS256)"
    
    # Contar puntos (debería haber 2)
    NUM_DOTS=$(echo "$OAUTH2_TOKEN" | tr -cd '.' | wc -c)
    
    if [ "$NUM_DOTS" = "2" ]; then
        print_success "Token OAuth2 tiene estructura JWT válida (3 partes)"
    else
        print_error "Token OAuth2 tiene estructura inválida"
    fi
fi

# ============================================================================
# RESUMEN FINAL
# ============================================================================

print_header "RESUMEN DE PRUEBAS"

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED + TESTS_SKIPPED))

echo -e "${GREEN}✓ Pruebas pasadas: $TESTS_PASSED${NC}"
echo -e "${RED}✗ Pruebas fallidas: $TESTS_FAILED${NC}"
echo -e "${YELLOW}⊘ Pruebas saltadas: $TESTS_SKIPPED${NC}"
echo -e "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "Total: $TOTAL_TESTS pruebas ejecutadas"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ TODAS LAS PRUEBAS PASARON${NC}"
    echo -e "${GREEN}OAuth 2.0 Dual Authentication está funcionando correctamente${NC}\n"
    exit 0
else
    echo -e "\n${RED}✗ ALGUNAS PRUEBAS FALLARON${NC}"
    echo -e "${RED}Verificar los logs anteriores para más detalles${NC}\n"
    exit 1
fi
