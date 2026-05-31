package com.distribuidos.usuario_service.service;

import com.distribuidos.usuario_service.dto.*;
import com.distribuidos.usuario_service.model.Role;
import com.distribuidos.usuario_service.model.User;
import com.distribuidos.usuario_service.repository.RoleRepository;
import com.distribuidos.usuario_service.repository.UserRepository;
import com.distribuidos.usuario_service.security.JwtService;
import com.distribuidos.usuario_service.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuditService auditService;
    
    public UserService(UserRepository userRepository, RoleRepository roleRepository, JwtService jwtService, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    /**
     * Verificar si un username está disponible (no existe)
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Verificar si un email está disponible (no existe)
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Borrado lógico de usuario (marca como inactivo)
     */
    @Transactional
    public void deleteUser(UUID id, UUID adminId) {
        if (id.equals(adminId)) {
            throw new RuntimeException("No puedes eliminar tu propio usuario");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setActive(false);
        userRepository.save(user);

        auditService.registrarCambioEstadoUsuario(id, "INACTIVO");
    }
    
    /**
     * AUTENTICACIÓN DE USUARIO (Login)
     * 
     * 1. Buscar usuario por username
     * 2. Verificar que esté activo
     * 3. Comparar password (SHA-512)
     * 4. Generar JWT
     * 5. Actualizar último login
     * 6. Registrar en auditoría
     * 
     * @throws RuntimeException si credenciales inválidas
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Buscar usuario
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Verificar activo
        if (!user.getActive()) {
            throw new RuntimeException("Usuario desactivado");
        }
        
        // Verificar password con SHA-512
        if (!SecurityUtils.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        
        // Actualizar último login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        // Registrar en auditoría
        auditService.registrarLogin(user.getId(), user.getUsername());
        
        // Generar JWT
        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().getName()
        );
        
        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .user(mapToResponse(user))
                .build();
    }
    
    /**
     * CREAR USUARIO (Solo ADMIN)
     * 
     * Reglas:
     * - Username único
     * - Email único
     * - Rol debe existir (ADMINISTRADOR, FUNCIONARIO, AUDITOR)
     * - Password se hashea con SHA-512
     */
    @Transactional
    public UserResponse createUser(UserRequest request) {
        return createUser(request, true);
    }

    /**
     * CREAR USUARIO con estado inicial configurable.
     * - activeByDefault=true: flujo interno/admin
     * - activeByDefault=false: registro público pendiente de aprobación
     */
    @Transactional
    public UserResponse createUser(UserRequest request, boolean activeByDefault) {
        // Validar username único
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El username ya existe");
        }
        
        // Validar email único
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya existe");
        }
        
        // Buscar rol
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Rol no válido: " + request.getRole()));
        
        // Crear usuario
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(SecurityUtils.hashSHA512(request.getPassword()));  // SHA-512!
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setActive(activeByDefault);
        
        User saved = userRepository.save(user);
        
        // Registrar en auditoría
        auditService.registrarCreacionUsuario(saved.getId(), saved.getUsername(), saved.getEmail());
        
        return mapToResponse(saved);
    }
    
    /**
     * LISTAR USUARIOS
     * Solo ADMIN puede ver todos
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * OBTENER USUARIO POR ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return mapToResponse(user);
    }
    
    /**
     * OBTENER USUARIO ACTUAL (para /auth/me)
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        return getUserById(userId);
    }
    
    /**
     * ACTUALIZAR USUARIO
     * Solo ADMIN puede actualizar: email, fullName, role
     */
    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request, UUID adminId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Actualizar email si se proporciona
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new RuntimeException("El email ya está registrado");
            }
            user.setEmail(request.getEmail());
        }
        
        // Actualizar fullName si se proporciona
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        
        // Actualizar rol si se proporciona
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            Role role = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new RuntimeException("Rol no válido: " + request.getRole()));
            user.setRole(role);
        }
        
        User saved = userRepository.save(user);
        
        // Registrar en auditoría
        String cambios = String.format("Email: %s, Nombre: %s, Rol: %s", 
            request.getEmail(), request.getFullName(), request.getRole());
        auditService.registrarModificacionUsuario(id, cambios);
        
        return mapToResponse(saved);
    }
    
    /**
     * CAMBIAR ESTADO DE USUARIO (Activar/Desactivar)
     * Solo ADMIN. No puede desactivarse a sí mismo.
     */
    @Transactional
    public UserResponse toggleUserStatus(UUID id, UUID adminId) {
        // No auto-desactivación
        if (id.equals(adminId)) {
            throw new RuntimeException("No puedes desactivar tu propio usuario");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setActive(!user.getActive());
        User saved = userRepository.save(user);
        
        // Registrar en auditoría
        String nuevoEstado = saved.getActive() ? "ACTIVO" : "INACTIVO";
        auditService.registrarCambioEstadoUsuario(id, nuevoEstado);
        
        return mapToResponse(saved);
    }
    
    /**
     * Mapper: Convierte Entity a DTO Response
     */
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}