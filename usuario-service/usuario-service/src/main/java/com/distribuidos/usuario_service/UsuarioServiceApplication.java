package com.distribuidos.usuario_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Microservicio de Usuarios y Autenticación (Auth)
 * Puerto: 8081
 * 
 * Responsabilidades:
 * - Login con JWT
 * - Gestión de usuarios (CRUD)
 * - Validación de SHA-512
 * - Roles: ADMINISTRADOR, FUNCIONARIO, AUDITOR
 */
@SpringBootApplication
@EnableDiscoveryClient  // Se registra automáticamente en Eureka
public class UsuarioServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsuarioServiceApplication.class, args);
		
		System.out.println("========================================");
		System.out.println("✅ USUARIO SERVICE INICIADO");
		System.out.println("📍 URL: http://localhost:8081");
		System.out.println("📋 Endpoints disponibles:");
		System.out.println("   POST /api/auth/login    (Login - Público)");
		System.out.println("   POST /api/auth/register (Crear usuario - Admin)");
		System.out.println("   GET  /api/auth/me       (Perfil actual)");
		System.out.println("   GET  /api/users         (Listar usuarios)");
		System.out.println("========================================");
	}
}
