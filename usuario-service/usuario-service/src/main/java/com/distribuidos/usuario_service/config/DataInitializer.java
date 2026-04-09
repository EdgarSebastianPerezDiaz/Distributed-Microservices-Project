package com.distribuidos.usuario_service.config;

import com.distribuidos.usuario_service.model.Role;
import com.distribuidos.usuario_service.model.User;
import com.distribuidos.usuario_service.repository.RoleRepository;
import com.distribuidos.usuario_service.repository.UserRepository;
import com.distribuidos.usuario_service.security.SecurityUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clase de inicialización de datos del sistema.
 *
 * <p>Esta configuración se ejecuta automáticamente al iniciar el microservicio
 * y tiene como objetivo garantizar que existan los datos mínimos necesarios
 * para el funcionamiento del sistema.</p>
 *
 * <h2>Responsabilidades:</h2>
 * <ul>
 *     <li>Crear los roles base del sistema si no existen</li>
 *     <li>Crear un usuario administrador por defecto si no hay usuarios registrados</li>
 * </ul>
 *
 * <h2>Datos iniciales creados:</h2>
 * <ul>
 *     <li>Roles: ADMINISTRADOR, FUNCIONARIO, AUDITOR</li>
 *     <li>Usuario admin:
 *          <ul>
 *              <li>username: admin</li>
 *              <li>password: admin123 (hasheado con SHA-512)</li>
 *          </ul>
 *     </li>
 * </ul>
 *
 * <h2>Notas importantes:</h2>
 * <ul>
 *     <li>La contraseña generada es solo para desarrollo y debe cambiarse en producción</li>
 *     <li>Se utiliza {@link SecurityUtils} para el hash de contraseñas</li>
 *     <li>La operación es transaccional para garantizar consistencia</li>
 * </ul>
 *
 * @author Dev1 - Servicio de Usuarios - Lina Ladino
 * @version 1.0
 */
@Configuration
public class DataInitializer {
    
    /**
     * Inicializa datos al arranque de la aplicación.
     *
     * @param roleRepo repositorio de roles
     * @param userRepo repositorio de usuarios
     * @return CommandLineRunner que ejecuta la lógica de inicialización
     */
    @Bean
    @Transactional
    CommandLineRunner initData(RoleRepository roleRepo, UserRepository userRepo) {
        return args -> {
            
            // ==============================
            // 1. CREACIÓN DE ROLES BASE
            // ==============================
            if (roleRepo.count() == 0) {
                System.out.println("Creando roles iniciales del sistema...");
                
                Role adminRole = new Role();
                adminRole.setName("ADMINISTRADOR");
                adminRole.setDescription("Superusuario del sistema");
                
                Role funcRole = new Role();
                funcRole.setName("FUNCIONARIO");
                funcRole.setDescription("Usuario operativo");
                
                Role audRole = new Role();
                audRole.setName("AUDITOR");
                audRole.setDescription("Acceso de solo lectura para auditoría");
                
                roleRepo.save(adminRole);
                roleRepo.save(funcRole);
                roleRepo.save(audRole);
                
                System.out.println("Roles creados correctamente");
            }
            
            // ==============================
            // 2. CREACIÓN DE USUARIO ADMIN
            // ==============================
            if (userRepo.count() == 0) {
                System.out.println(" Creando usuario admin por defecto...");
                
                Role adminRole = roleRepo.findByName("ADMINISTRADOR").orElseThrow();
                
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(SecurityUtils.hashSHA512("admin123")); 
                admin.setEmail("admin@uptc.edu.co");
                admin.setFullName("Administrador del Sistema");
                admin.setRole(adminRole);
                admin.setActive(true);
                
                userRepo.save(admin);
                
                System.out.println("   Usuario admin creado:");
                System.out.println("   Username: admin");
                System.out.println("   Password: admin123");
            }
        };
    }
}