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
 * Inicializador de datos
 * 
 * Se ejecuta al arrancar la aplicación.
 * Crea los 3 roles y usuarios por defecto si no existen.
 */
@Configuration
public class DataInitializer {
    
    @Bean
    @Transactional
    CommandLineRunner initData(RoleRepository roleRepo, UserRepository userRepo) {
        return args -> {
            // 1. Crear roles si no existen
            if (roleRepo.count() == 0) {
                System.out.println("🔄 Creando roles...");
                
                Role adminRole = new Role();
                adminRole.setName("ADMINISTRADOR");
                adminRole.setDescription("Superusuario del sistema");
                
                Role funcRole = new Role();
                funcRole.setName("FUNCIONARIO");
                funcRole.setDescription("Usuario operativo");
                
                Role audRole = new Role();
                audRole.setName("AUDITOR");
                audRole.setDescription("Solo lectura para auditoría");
                
                roleRepo.save(adminRole);
                roleRepo.save(funcRole);
                roleRepo.save(audRole);
                
                System.out.println("✅ Roles creados: ADMINISTRADOR, FUNCIONARIO, AUDITOR");
            }
            
            // 2. Crear usuario admin por defecto si no existe
            if (userRepo.count() == 0) {
                System.out.println("🔄 Creando usuario admin por defecto...");
                
                Role adminRole = roleRepo.findByName("ADMINISTRADOR").orElseThrow();
                
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(SecurityUtils.hashSHA512("admin123"));  // Cambiar en producción!
                admin.setEmail("admin@uptc.edu.co");
                admin.setFullName("Administrador del Sistema");
                admin.setRole(adminRole);
                admin.setActive(true);
                
                userRepo.save(admin);
                
                System.out.println("✅ Usuario admin creado:");
                System.out.println("   Username: admin");
                System.out.println("   Password: admin123");
                System.out.println("   ⚠️  Cambia esta contraseña en producción!");
            }

            // 3. Crear usuario auditor por defecto si no existe
            if (!userRepo.existsByUsername("auditor")) {
                System.out.println("🔄 Creando usuario auditor por defecto...");

                Role audRole = roleRepo.findByName("AUDITOR").orElseThrow();

                User auditor = new User();
                auditor.setUsername("auditor");
                auditor.setPasswordHash(SecurityUtils.hashSHA512("auditor123"));
                auditor.setEmail("auditor@uptc.edu.co");
                auditor.setFullName("Auditor del Sistema");
                auditor.setRole(audRole);
                auditor.setActive(true);

                userRepo.save(auditor);

                System.out.println("✅ Usuario auditor creado:");
                System.out.println("   Username: auditor");
                System.out.println("   Password: auditor123");
                System.out.println("   ⚠️  Cambia esta contraseña en producción!");
            }
        };
    }
}