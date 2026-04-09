package com.distribuidos.usuario_service.controller;

import com.distribuidos.usuario_service.dto.*;
import com.distribuidos.usuario_service.security.JwtService;
import com.distribuidos.usuario_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la gestión de autenticación y usuarios.
 *
 * <p>Este controlador expone los endpoints necesarios para:
 * autenticación (login) y administración de usuarios dentro del sistema.</p>
 *
 * <h2>Responsabilidades:</h2>
 * <ul>
 *     <li>Autenticación de usuarios mediante JWT</li>
 *     <li>Creación de nuevos usuarios (solo ADMIN)</li>
 *     <li>Consulta de información del usuario autenticado</li>
 *     <li>Gestión de usuarios (listar, consultar, activar/desactivar)</li>
 * </ul>
 *
 * <h2>Seguridad:</h2>
 * <ul>
 *     <li>Login es público</li>
 *     <li>El resto de endpoints requieren JWT válido</li>
 *     <li>Algunos endpoints requieren rol ADMINISTRADOR</li>
 * </ul>
 *
 * <h2>Base URL:</h2>
 * <pre>/api/auth</pre>
 *
 * @author Dev1 - servicio de usuario - Lina Ladino
 * @version 1.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;

    /**
     * Endpoint de autenticación (login).
     *
     * <p>Recibe credenciales (username y password) y devuelve un token JWT
     * si la autenticación es exitosa.</p>
     *
     * @param request credenciales del usuario
     * @return token JWT y datos del usuario autenticado
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para registrar un nuevo usuario.
     *
     * <p>Solo accesible para usuarios con rol ADMINISTRADOR.</p>
     *
     * @param request datos del nuevo usuario
     * @return usuario creado
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Obtiene los datos del usuario autenticado.
     *
     * <p>Extrae el userId desde el token JWT enviado en el header Authorization.</p>
     *
     * @param authHeader header Authorization con formato Bearer
     * @return datos del usuario actual
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);
        
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }

    /**
     * Lista todos los usuarios del sistema.
     *
     * <p>Solo accesible para ADMINISTRADOR.</p>
     *
     * @return lista de usuarios
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<java.util.List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Obtiene un usuario específico por su ID.
     *
     * <p>Solo accesible para ADMINISTRADOR.</p>
     *
     * @param id identificador UUID del usuario
     * @return usuario encontrado
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Cambia el estado de un usuario (activar/desactivar).
     *
     * <p>Solo accesible para ADMINISTRADOR.
     * Impide que un administrador se desactive a sí mismo.</p>
     *
     * @param id ID del usuario a modificar
     * @param authHeader token del administrador
     * @return usuario actualizado
     */
    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> toggleStatus(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID adminId = jwtService.extractUserId(token);
        
        return ResponseEntity.ok(userService.toggleUserStatus(id, adminId));
    }
}