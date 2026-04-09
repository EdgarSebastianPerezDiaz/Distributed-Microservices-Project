package com.distribuidos.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * 🚀 Eureka Server - Registro y Descubrimiento de Microservicios
 *
 * Este componente actúa como el **servidor central de descubrimiento**
 * dentro de la arquitectura de microservicios del sistema.
 *
 * 📌 Responsabilidades principales:
 * - Registrar automáticamente todos los microservicios al iniciar
 * - Mantener un catálogo actualizado de servicios disponibles
 * - Permitir que los servicios se descubran entre sí dinámicamente
 * - Facilitar el balanceo de carga (junto con el Gateway)
 *
 * 📌 ¿Cómo funciona?
 * 1. Cada microservicio (usuario-service, proveedor-service, etc.)
 *    se registra en Eureka al arrancar.
 * 2. Eureka guarda su información (nombre, IP, puerto).
 * 3. Cuando un servicio necesita comunicarse con otro:
 *    → Consulta a Eureka
 *    → Obtiene la ubicación del servicio destino
 * 4. La comunicación se realiza sin necesidad de URLs fijas.
 *
 * 📌 Beneficios:
 * - Desacoplamiento entre microservicios
 * - Escalabilidad (múltiples instancias)
 * - Tolerancia a fallos
 *
 * 📌 Dashboard:
 * - URL: http://localhost:8761
 * - Permite visualizar todos los servicios registrados en tiempo real
 *
 * ⚠️ Nota:
 * Este servicio debe estar activo antes de iniciar los demás microservicios.
 *
 * @author Dev1 - Infraestructura - Lina Ladino
 * @version 1.0
 */
@SpringBootApplication
@EnableEurekaServer  // Habilita este proyecto como servidor Eureka
public class EurekaServerApplication {

    /**
     * Método principal que inicia el servidor Eureka.
     */
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);

        // Logs informativos para verificar arranque correcto
        System.out.println("========================================");
        System.out.println("EUREKA SERVER INICIADO EXITOSAMENTE");
        System.out.println("Dashboard: http://localhost:8761");
        System.out.println("========================================");
    }
}