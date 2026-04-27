package com.distribuidos.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server - Servidor de Registro de Microservicios
 * 
 * Este servicio es el "directorio central" donde todos los microservicios
 * (Usuario, Proveedor, Contrato, Auditoría, Gateway) se registran.
 * 
 * Anotación @EnableEurekaServer: Convierte esta aplicación en un servidor
 * de descubrimiento de servicios.
 * 
 * Flujo:
 * 1. Cada microservicio arranca y se registra aquí con su nombre y puerto
 * 2. Cuando un servicio necesita llamar a otro, pregunta a Eureka dónde está
 * 3. Eureka responde con la ubicación (IP:Puerto) del servicio solicitado
 * 
 * @author Dev1 - Infraestructura - Lina Ladino
 * @version 1.0
 */
@SpringBootApplication
@EnableEurekaServer  // Activa el servidor Eureka
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("✅ EUREKA SERVER INICIADO EXITOSAMENTE");
        System.out.println("📍 URL del Dashboard: http://localhost:8761");
        System.out.println("========================================");
    }
}