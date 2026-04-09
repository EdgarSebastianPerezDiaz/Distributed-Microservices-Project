package com.distribuidos.usuario_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 🚀 Usuario Service - Microservicio de Usuarios y Autenticación
 *
 * Este microservicio es responsable de la gestión de usuarios del sistema
 * y del proceso de autenticación mediante JWT.
 *
 * 📌 Responsabilidades principales:
 * - Autenticación de usuarios (login)
 * - Generación y validación de tokens JWT
 * - Gestión de usuarios (crear, consultar, activar/desactivar)
 * - Administración de roles del sistema
 *
 * 📌 Roles manejados:
 * - ADMINISTRADOR → Control total del sistema
 * - FUNCIONARIO   → Operación de procesos (contratos, etc.)
 * - AUDITOR       → Acceso de solo lectura
 *
 * 📌 Seguridad:
 * - Contraseñas almacenadas con hash SHA-512
 * - Autenticación basada en JWT
 * - Autorización mediante roles (Spring Security)
 *
 * 📌 Integración:
 * - Se registra automáticamente en Eureka
 * - Es consumido a través del API Gateway
 *
 * 📌 Puerto por defecto:
 * - http://localhost:8084
 *
 * ⚠️ Importante:
 * - Este servicio NO debe ser consumido directamente por clientes externos
 * - Todas las peticiones deben pasar por el API Gateway
 *
 * @author Dev1 - Servicio de Usuarios - Lina Ladino
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient  // Permite registrarse en Eureka para descubrimiento de servicios
public class UsuarioServiceApplication {

    /**
     * Método principal que inicia el microservicio de usuarios.
     */
    public static void main(String[] args) {
        SpringApplication.run(UsuarioServiceApplication.class, args);

        // Logs informativos para validar arranque correcto
        System.out.println("========================================");
        System.out.println("USUARIO SERVICE INICIADO");
        System.out.println("URL: http://localhost:8084");
        System.out.println("Endpoints disponibles:");
        System.out.println("   POST /api/auth/login    (Login - Público)");
        System.out.println("   POST /api/auth/register (Crear usuario - Admin)");
        System.out.println("   GET  /api/auth/me       (Perfil actual)");
        System.out.println("   GET  /api/users         (Listar usuarios)");
        System.out.println("========================================");
    }
}