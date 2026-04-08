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
 * Controlador de Autenticación y Usuarios
 * 
 * Endpoints:
 * 
 * PÚBLICOS:
 * POST /api/auth/login       - Login (devuelve JWT)
 * 
 * PROTEGIDOS (requieren JWT):
 * GET  /api/auth/me          - Datos del usuario actual
 * POST /api/auth/register    - Crear usuario (solo ADMIN)
 * GET  /api/users            - Listar usuarios (solo ADMIN)
 * GET  /api/users/{id}       - Ver usuario específico
 * PATCH /api/users/{id}/status - Activar/Desactivar (solo ADMIN)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
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
     * REGISTRAR USUARIO - SOLO ADMIN
     * Requiere token JWT con rol ADMINISTRADOR
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
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
     * CAMBIAR ESTADO (ACTIVAR/DESACTIVAR) - SOLO ADMIN
     */
    @PatchMapping("/users/{id}/status")
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