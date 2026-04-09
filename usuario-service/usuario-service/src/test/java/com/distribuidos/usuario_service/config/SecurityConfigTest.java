package com.distribuidos.usuario_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de carga de contexto para SecurityConfig.
 *
 * Verifica que la configuración de seguridad se carga correctamente
 * sin errores en el contexto de Spring.
 */
@SpringBootTest
class SecurityConfigTest {

    @Test
    void contextLoads() {
        // ==============================
        // PROBANDO:
        // Que la configuración de seguridad no rompe el arranque de la aplicación
        // ==============================

        // Si el contexto carga sin excepciones → test pasa
    }
}