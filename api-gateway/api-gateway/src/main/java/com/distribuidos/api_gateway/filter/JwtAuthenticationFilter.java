package com.distribuidos.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Date;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 🔐 JwtAuthenticationFilter - Filtro global de autenticación en el API Gateway
 *
 * Este filtro intercepta **todas las peticiones HTTP** que pasan por el Gateway
 * y se encarga de validar el token JWT antes de permitir el acceso a los microservicios.
 *
 * 📌 Responsabilidades:
 * - Validar la existencia del token JWT en el header Authorization
 * - Verificar la firma del token (HS512)
 * - Validar la expiración del token
 * - Extraer información del usuario (user_id, role)
 * - Propagar datos al microservicio mediante headers HTTP
 *
 * 📌 Flujo:
 * 1. Se intercepta la petición entrante
 * 2. Se valida si la ruta es pública
 * 3. Si NO es pública:
 *    - Se valida el token JWT
 *    - Se extraen los claims
 *    - Se agregan headers personalizados
 * 4. Se reenvía la petición al microservicio correspondiente
 *
 * 📌 Headers agregados:
 * - X-User-Id   → ID del usuario autenticado
 * - X-User-Role → Rol del usuario
 * - X-Role      → Rol (compatibilidad con otros servicios)
 *
 * 📌 Rutas públicas:
 * - /api/auth/login
 * - /actuator/**
 * - /eureka/**
 * - /swagger/**
 * - /v3/api-docs/**
 *
 * ⚠️ Importante:
 * - Este filtro NO valida permisos (roles), solo autenticación
 * - La autorización se maneja en cada microservicio (Spring Security)
 *
 * @author Dev1 - Infraestructura - Lina Ladino
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    // Clave secreta (debe ser EXACTAMENTE igual en todos los servicios)
    private static final String SECRET_KEY = "mi-clave-super-secreta-para-jwt-de-512-bits-minimo-requerido-para-firmar-tokens-seguros-en-el-sistema-de-contratos-uptc-2026";
    // Generación de clave segura para validar JWT
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    // Rutas públicas (no requieren autenticación)
    private final List<String> publicPaths = List.of(
        "/api/auth/login",
        "/actuator",
        "/eureka",
        "/swagger",
        "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // Rutas públicas (no requieren autenticación)
        System.out.println("========================================");
        System.out.println(">>> JWT FILTER EJECUTÁNDOSE");
        System.out.println(">>> Path: " + path);
        System.out.println(">>> Headers completos: " + exchange.getRequest().getHeaders());
        System.out.println(">>> Authorization header: " + exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        System.out.println("========================================");

        System.out.println(">>> ¿Es pública?: " + isPublicPath(path));

        // Permitir rutas públicas sin validación
        if (isPublicPath(path)) {
            System.out.println(">>> RUTA PÚBLICA - permitiendo");
            return chain.filter(exchange);
        }

        // Obtener header Authorization
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        // Validar formato del token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Token requerido");
        }

        String token = authHeader.substring(7);

        try {
            // Validar formato del token
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
           
            // Validar expiración
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                return unauthorized(exchange, "Token expirado");
            }

            // Extraer información del usuario
            String userId = claims.get("user_id", String.class);
            String role = claims.get("role", String.class);

            // Propagar datos al microservicio
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return unauthorized(exchange, "Token inválido");
        }
    }

    /**
     * Verifica si la ruta es pública
     */
    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(p -> path.startsWith(p));
    }

    /**
     * Respuesta HTTP 401 - No autorizado
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"error\":\"%s\",\"timestamp\":\"%s\"}", 
            message, java.time.Instant.now());
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory().wrap(body.getBytes())));
    }

    /**
     * Prioridad del filtro (más bajo = mayor prioridad)
     */
    @Override
    public int getOrder() {
        return -1000;
    }
}