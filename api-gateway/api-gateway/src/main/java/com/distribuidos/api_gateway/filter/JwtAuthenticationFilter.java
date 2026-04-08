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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String SECRET_KEY = "mi-clave-super-secreta-para-jwt-de-512-bits-minimo-requerido-para-firmar-tokens-seguros-en-el-sistema-de-contratos-uptc-2026";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    private final List<String> publicPaths = List.of(
        "/auth",
        "/api/auth",
        "/actuator",
        "/eureka",
        "/swagger",
        "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // DEBUG COMPLETO
        System.out.println("========================================");
        System.out.println(">>> JWT FILTER EJECUTÁNDOSE");
        System.out.println(">>> Path: " + path);
        System.out.println(">>> Headers completos: " + exchange.getRequest().getHeaders());
        System.out.println(">>> Authorization header: " + exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        System.out.println("========================================");

        System.out.println(">>> ¿Es pública?: " + isPublicPath(path));

        if (isPublicPath(path)) {
            System.out.println(">>> RUTA PÚBLICA - permitiendo");
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Token requerido");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return unauthorized(exchange, "Token inválido");
        }
    }

    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(p -> path.startsWith(p));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"error\":\"%s\",\"timestamp\":\"%s\"}", 
            message, java.time.Instant.now());
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -1000; // Máxima prioridad
    }
}