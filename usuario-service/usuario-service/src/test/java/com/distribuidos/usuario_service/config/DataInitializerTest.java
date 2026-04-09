package com.distribuidos.usuario_service.config;

import com.distribuidos.usuario_service.repository.RoleRepository;
import com.distribuidos.usuario_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración para verificar la inicialización de datos.
 */
@SpringBootTest
class DataInitializerTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldInitializeRolesAndAdminUser() {
        // Verificar que existen roles
        assertThat(roleRepository.count()).isGreaterThanOrEqualTo(3);

        // Verificar que existe al menos un usuario
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(1);

        // Verificar que existe admin
        assertThat(userRepository.findByUsername("admin")).isPresent();
    }
}