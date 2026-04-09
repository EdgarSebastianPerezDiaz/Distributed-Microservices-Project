package com.distribuidos.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 🚀 API Gateway - Punto de Entrada del Sistema
 *
 * Este componente actúa como la **puerta de entrada única** para todos los clientes
 * (frontend, Postman, navegador, etc.) dentro de la arquitectura de microservicios.
 *
 * 📌 Responsabilidades principales:
 * - Enrutamiento dinámico de peticiones hacia los microservicios
 * - Descubrimiento de servicios mediante Eureka
 * - Balanceo de carga entre instancias
 * - Seguridad centralizada (validación de JWT)
 *
 * 📌 Flujo de una petición:
 * 1. Cliente realiza petición HTTP (ej: /api/users)
 * 2. El Gateway intercepta la petición
 * 3. Valida el token JWT (si aplica)
 * 4. Consulta a Eureka para ubicar el microservicio destino
 * 5. Redirige la petición al servicio correspondiente
 * 6. Retorna la respuesta al cliente
 *
 * 📌 Rutas manejadas:
 * - /api/auth/**        → usuario-service (autenticación)
 * - /api/users/**       → usuario-service (gestión de usuarios)
 * - /api/suppliers/**   → proveedor-service
 * - /api/contracts/**   → contrato-service
 * - /api/audit/**       → auditoria-service
 *
 * 📌 Seguridad:
 * Se deshabilita la configuración automática de Spring Security ya que
 * la autenticación se maneja manualmente mediante filtros JWT personalizados.
 *
 * ⚠️ Importante:
 * - Todas las peticiones externas deben pasar por este Gateway
 * - Ningún cliente debe consumir directamente los microservicios
 *
 * @author Dev1 - Infraestructura - Lina Ladino
 * @version 1.0
 */
@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    ReactiveSecurityAutoConfiguration.class
})
@EnableDiscoveryClient  // Permite registrarse y descubrir servicios en Eureka
public class ApiGatewayApplication {

    /**
     * Método principal que inicia el API Gateway.
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);

        // Logs informativos para verificar arranque correcto
        System.out.println("========================================");
        System.out.println("API GATEWAY INICIADO EXITOSAMENTE");
        System.out.println("URL Base: http://localhost:8081");
        System.out.println("Rutas disponibles:");
        System.out.println("   - /api/auth/**       → Usuario Service");
        System.out.println("   - /api/users/**      → Usuario Service");
        System.out.println("   - /api/suppliers/**  → Proveedor Service");
        System.out.println("   - /api/contracts/**  → Contrato Service");
        System.out.println("   - /api/audit/**      → Auditoria Service");
        System.out.println("========================================");
    }
}