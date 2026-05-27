package com.distribuidos.usuario_service.controller;

import com.distribuidos.usuario_service.dto.*;
import com.distribuidos.usuario_service.security.JwtService;
import com.distribuidos.usuario_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;

/**
 * Controlador de Autenticación y Usuarios
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }
    
    /**
     * LOGIN - PÚBLICO
     * No requiere autenticación previa
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * REGISTRAR USUARIO - PÚBLICO
     * Permite crear una cuenta para acceder al sistema.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        // Registro público: usuario queda inactivo hasta aprobación administrativa.
        UserResponse user = userService.createUser(request, false);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Validar disponibilidad de username
     * GET /api/auth/users/validate/username?username=xxx
     */
    @GetMapping("/users/validate/username")
    public ResponseEntity<Map<String, Boolean>> validateUsername(@RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Validar disponibilidad de email
     * GET /api/auth/users/validate/email?email=xxx
     */
    @GetMapping("/users/validate/email")
    public ResponseEntity<Map<String, Boolean>> validateEmail(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Eliminar (baja lógica) de usuario - SOLO ADMIN
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UUID adminId = jwtService.extractUserId(token);
        userService.deleteUser(id, adminId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * OBTENER USUARIO ACTUAL
     * Extrae el userId del token JWT y devuelve sus datos
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        // Extraer token del header "Bearer <token>"
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);
        
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }
    
    /**
     * LISTAR TODOS LOS USUARIOS - SOLO ADMIN
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<java.util.List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    /**
     * OBTENER USUARIO POR ID
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * ACTUALIZAR USUARIO - SOLO ADMIN
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        // Extraer adminId del token para auditoría
        String token = authHeader.replace("Bearer ", "");
        UUID adminId = jwtService.extractUserId(token);
        
        return ResponseEntity.ok(userService.updateUser(id, request, adminId));
    }

    /**
     * CAMBIAR ESTADO (ACTIVAR/DESACTIVAR) - SOLO ADMIN
     */
    @PatchMapping("/users/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> toggleStatus(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        
        // Extraer adminId del token para evitar auto-desactivación
        String token = authHeader.replace("Bearer ", "");
        UUID adminId = jwtService.extractUserId(token);
        
        return ResponseEntity.ok(userService.toggleUserStatus(id, adminId));
    }
}