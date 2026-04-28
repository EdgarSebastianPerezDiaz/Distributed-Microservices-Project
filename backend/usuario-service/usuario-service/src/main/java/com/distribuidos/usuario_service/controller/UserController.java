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

/**
 * Controlador de Usuarios
 * Maneja operaciones CRUD y gestión de usuarios (solo ADMIN)
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }
    
    /**
     * LISTAR TODOS LOS USUARIOS - SOLO ADMIN
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<java.util.List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    /**
     * OBTENER USUARIO POR ID - SOLO ADMIN
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
    
    /**
     * ACTUALIZAR USUARIO - SOLO ADMIN
     * PUT /api/users/{id}
     * Permite actualizar: email, fullName, role
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        // Extraer adminId del token para auditoria
        String token = authHeader.replace("Bearer ", "");
        UUID adminId = jwtService.extractUserId(token);
        
        return ResponseEntity.ok(userService.updateUser(id, request, adminId));
    }
    
    /**
     * CAMBIAR ESTADO (ACTIVAR/DESACTIVAR) - SOLO ADMIN
     * PATCH /api/users/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponse> toggleStatus(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        
        // Extraer adminId del token para evitar auto-desactivación
        String token = authHeader.replace("Bearer ", "");
        UUID adminId = jwtService.extractUserId(token);
        
        return ResponseEntity.ok(userService.toggleUserStatus(id, adminId));
    }
    
    /**
     * OBTENER USUARIO ACTUAL (para perfil)
     * GET /api/users/me
     * Nota: Esta ruta debe estar en AuthController
     */
    // Nota: Implementado en AuthController como GET /api/auth/me
}
