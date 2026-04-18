package com.distribuidos.usuario_service.service;

import com.distribuidos.usuario_service.dto.*;
import com.distribuidos.usuario_service.client.AuditClient;
import com.distribuidos.usuario_service.model.Role;
import com.distribuidos.usuario_service.model.User;
import com.distribuidos.usuario_service.repository.RoleRepository;
import com.distribuidos.usuario_service.repository.UserRepository;
import com.distribuidos.usuario_service.security.JwtService;
import com.distribuidos.usuario_service.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j  
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuditClient auditClient;
    
    public UserService(UserRepository userRepository, RoleRepository roleRepository, JwtService jwtService,AuditClient auditClient) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.auditClient=auditClient;
    }
    
    /**
     * AUTENTICACIÓN DE USUARIO (Login)
     * 
     * 1. Buscar usuario por username
     * 2. Verificar que esté activo
     * 3. Comparar password (SHA-512)
     * 4. Generar JWT
     * 5. Actualizar último login
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
    public UserResponse createUser(UserRequest request,String adminToken) {
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
        user.setActive(true);


         UUID adminId     = jwtService.extractUserId(adminToken);
        String adminName = jwtService.extractUsername(adminToken);
        String adminRole = jwtService.extractRole(adminToken);

         User saved = userRepository.save(user);
       

        sendAuditEvent(
                saved.getId().toString(),
                "CREAR_USUARIO",
                null,
                "ACTIVO",
                "Nuevo usuario creado: " + saved.getUsername() + " con rol " + saved.getRole().getName(),
                adminId.toString(),
                adminName,
                adminRole,
                1,
                adminToken
        );

         return mapToResponse(saved);
       
    }


    //Auditoria

     private void sendAuditEvent(String entidadId, String tipoEvento,
                                 String estadoAnterior, String estadoNuevo,
                                 String descripcion, String usuarioId,
                                 String usuarioNombre, String rolUsuario,
                                 int version, String token) {
        try {
            AuditEventDTO evento = AuditEventDTO.builder()
                    .entidad_tipo("USUARIO")
                    .entidad_id(entidadId)
                    .tipo_evento(tipoEvento)
                    .descripcion(descripcion)
                    .estado_anterior(estadoAnterior)
                    .estado_nuevo(estadoNuevo)
                    .motivo(null)
                    .usuario_id(usuarioId)
                    .usuario_nombre(usuarioNombre)
                    .rol_usuario(rolUsuario)
                    .version(version)
                    .fecha(OffsetDateTime.now(ZoneOffset.UTC))
                    .contrato_id(null)
                    .build();

            auditClient.registrarEvento(evento, token);
        } catch (Exception e) {
            log.warn("Error enviando auditoría para usuario {}: {}", entidadId, e.getMessage());
        }
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
     * CAMBIAR ESTADO DE USUARIO (Activar/Desactivar)
     * Solo ADMIN. No puede desactivarse a sí mismo.
     * @param token 
     */
    @Transactional
    public UserResponse toggleUserStatus(UUID id, UUID adminId, String token) {
        // No auto-desactivación
        if (id.equals(adminId)) {
            throw new RuntimeException("No puedes desactivar tu propio usuario");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
         String estadoAnterior = user.getActive() ? "ACTIVO" : "INACTIVO";
        
         user.setActive(!user.getActive());
         User saved = userRepository.save(user);
        
         String adminName = jwtService.extractUsername(token);
        String adminRole = jwtService.extractRole(token);


        String estadoNuevo = saved.getActive() ? "ACTIVO" : "INACTIVO";

        sendAuditEvent(
            saved.getId().toString(),
            "MODIFICAR_USUARIO",
            estadoAnterior,
            estadoNuevo,
            "Estado de usuario '" + saved.getUsername() + "' cambiado de " + estadoAnterior + " a " + estadoNuevo,
            adminId.toString(),
            adminName,
            adminRole,
            1,
            token
    );
    log.info("Estado de usuario {} cambiado: {} → {}", id, estadoAnterior, estadoNuevo);
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