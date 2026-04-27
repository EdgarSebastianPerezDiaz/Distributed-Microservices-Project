package com.distribuidos.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway - Puerta de Entrada del Sistema
 * 
 * Este componente es el "portero" de la arquitectura de microservicios.
 * Todas las peticiones externas deben pasar por aquí.
 * 
 * Responsabilidades principales:
 * 1. Enrutamiento dinámico: Redirige /api/users/** al usuario-service, etc.
 * 2. Descubrimiento de servicios: Consulta a Eureka dónde están los servicios
 * 3. Balanceo de carga: Distribuye carga entre instancias múltiples
 * 4. Seguridad centralizada: Valida JWT antes de dejar pasar la petición
 * 
 * Flujo de una petición:
 * Cliente → Gateway → (Valida JWT) → Consulta Eureka → Redirige al Microservicio
 * 
 * @author Dev1 - Infraestructura - Lina Ladino
 * @version 1.0
 */
@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    ReactiveSecurityAutoConfiguration.class
})
@EnableDiscoveryClient  // Se registra en Eureka y consulta otros servicios
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        System.out.println("========================================");
        System.out.println("✅ API GATEWAY INICIADO EXITOSAMENTE");
        System.out.println("📍 URL Base: http://localhost:8081");
        System.out.println("📋 Rutas disponibles:");
        System.out.println("   - /api/auth/**   → Usuario Service");
        System.out.println("   - /api/users/**  → Usuario Service");
        System.out.println("   - /api/suppliers/** → Proveedor Service");
        System.out.println("   - /api/contracts/** → Contrato Service");
        System.out.println("   - /api/audit/**  → Auditoria Service");
        System.out.println("========================================");
    }
}