package com.distribuidos.usuario_service.service;

import com.distribuidos.usuario_service.client.AuditClient;
import com.distribuidos.usuario_service.dto.*;
import com.distribuidos.usuario_service.exception.*;
import com.distribuidos.usuario_service.model.Role;
import com.distribuidos.usuario_service.model.User;
import com.distribuidos.usuario_service.repository.RoleRepository;
import com.distribuidos.usuario_service.repository.UserRepository;
import com.distribuidos.usuario_service.security.JwtService;
import com.distribuidos.usuario_service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UserService
 * 
 * @author Dev1 - Infraestructura
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("UserService Tests")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private AuditClient auditClient;
    
    @InjectMocks
    private UserService userService;
    
    private UUID testUserId;
    private UUID adminId;
    private String testUsername;
    private String testPassword;
    private String testEmail;
    private User testUser;
    private Role adminRole;
    private String adminToken;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        testUsername = "testuser";
        testPassword = "password123";
        testEmail = "test@example.com";
        adminToken = "Bearer admin-token";
        
        // Setup Role
        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMINISTRADOR");
        
        // Setup User
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername(testUsername);
        testUser.setPasswordHash(SecurityUtils.hashSHA512(testPassword));
        testUser.setEmail(testEmail);
        testUser.setFullName("Test User");
        testUser.setRole(adminRole);
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("✅ Login exitoso con credenciales válidas")
    void testLogin_Success() {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testUsername)
                .password(testPassword)
                .build();
        
        when(userRepository.findByUsername(testUsername))
                .thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUserId, testUsername, "ADMINISTRADOR"))
                .thenReturn("jwt-token-123");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        
        // Act
        LoginResponse response = userService.login(loginRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals(testUsername, response.getUser().getUsername());
        
        verify(userRepository, times(1)).findByUsername(testUsername);
        verify(jwtService, times(1)).generateToken(testUserId, testUsername, "ADMINISTRADOR");
    }
    
    @Test
    @DisplayName("❌ Login falla si usuario no existe")
    void testLogin_UserNotFound() {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .username("nonexistent")
                .password(testPassword)
                .build();
        
        when(userRepository.findByUsername("nonexistent"))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.login(loginRequest);
        });
    }
    
    @Test
    @DisplayName("❌ Login falla si contraseña es incorrecta")
    void testLogin_InvalidPassword() {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testUsername)
                .password("wrongpassword")
                .build();
        
        when(userRepository.findByUsername(testUsername))
                .thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.login(loginRequest);
        });
    }
    
    @Test
    @DisplayName("❌ Login falla si usuario está inactivo")
    void testLogin_UserInactive() {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testUsername)
                .password(testPassword)
                .build();
        
        testUser.setActive(false);
        when(userRepository.findByUsername(testUsername))
                .thenReturn(Optional.of(testUser));
        
        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            userService.login(loginRequest);
        });
    }
    
    @Test
    @DisplayName("✅ Crear usuario exitosamente")
    void testCreateUser_Success() {
        // Arrange
        UserRequest userRequest = UserRequest.builder()
                .username("newuser")
                .password("SecurePass123!")
                .email("new@example.com")
                .fullName("New User")
                .role("ADMINISTRADOR")
                .build();
        
        when(userRepository.existsByUsername("newuser"))
                .thenReturn(false);
        when(userRepository.existsByEmail("new@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("ADMINISTRADOR"))
                .thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(jwtService.extractUserId(adminToken))
                .thenReturn(adminId);
        when(jwtService.extractUsername(adminToken))
                .thenReturn("admin");
        when(jwtService.extractRole(adminToken))
                .thenReturn("ADMINISTRADOR");
        
        // Act
        UserResponse response = userService.createUser(userRequest, adminToken);
        
        // Assert
        assertNotNull(response);
        assertEquals(testUsername, response.getUsername());
        
        // Se debería haber llamado al auditClient
        verify(auditClient, times(1)).registrarEvento(any(AuditEventDTO.class), anyString());
    }
    
    @Test
    @DisplayName("❌ Crear usuario falla si username ya existe")
    void testCreateUser_UsernameAlreadyExists() {
        // Arrange
        UserRequest userRequest = UserRequest.builder()
                .username("existing")
                .password("password")
                .email("new@example.com")
                .fullName("New User")
                .role("ADMINISTRADOR")
                .build();
        
        when(userRepository.existsByUsername("existing"))
                .thenReturn(true);
        
        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.createUser(userRequest, adminToken);
        });
    }
    
    @Test
    @DisplayName("❌ Crear usuario falla si rol es inválido")
    void testCreateUser_InvalidRole() {
        // Arrange
        UserRequest userRequest = UserRequest.builder()
                .username("newuser")
                .password("password")
                .email("new@example.com")
                .fullName("New User")
                .role("INVALID_ROLE")
                .build();
        
        when(userRepository.existsByUsername("newuser"))
                .thenReturn(false);
        when(userRepository.existsByEmail("new@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("INVALID_ROLE"))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(InvalidRoleException.class, () -> {
            userService.createUser(userRequest, adminToken);
        });
    }
    
    @Test
    @DisplayName("✅ Obtener usuario por ID")
    void testGetUserById_Success() {
        // Arrange
        when(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser));
        
        // Act
        UserResponse response = userService.getUserById(testUserId);
        
        // Assert
        assertNotNull(response);
        assertEquals(testUserId, response.getId());
        assertEquals(testUsername, response.getUsername());
        
        verify(userRepository, times(1)).findById(testUserId);
    }
    
    @Test
    @DisplayName("❌ Obtener usuario falla si no existe")
    void testGetUserById_NotFound() {
        // Arrange
        UUID nonexistentId = UUID.randomUUID();
        when(userRepository.findById(nonexistentId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(nonexistentId);
        });
    }
    
    @Test
    @DisplayName("✅ Listar todos los usuarios")
    void testGetAllUsers_Success() {
        // Arrange
        when(userRepository.findAll())
                .thenReturn(List.of(testUser));
        
        // Act
        List<UserResponse> response = userService.getAllUsers();
        
        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testUsername, response.get(0).getUsername());
        
        verify(userRepository, times(1)).findAll();
    }
    
    @Test
    @DisplayName("✅ Cambiar estado de usuario (desactivar)")
    void testToggleUserStatus_Success() {
        // Arrange
        UUID otherAdminId = UUID.randomUUID();
        when(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(jwtService.extractUsername(adminToken))
                .thenReturn("admin");
        when(jwtService.extractRole(adminToken))
                .thenReturn("ADMINISTRADOR");
        
        // Act
        UserResponse response = userService.toggleUserStatus(testUserId, otherAdminId, adminToken);
        
        // Assert
        assertNotNull(response);
        assertFalse(response.getActive()); // Should be toggled to inactive
        
        verify(auditClient, times(1)).registrarEvento(any(AuditEventDTO.class), anyString());
    }
    
    @Test
    @DisplayName("❌ No se puede desactivar a sí mismo")
    void testToggleUserStatus_SelfDeactivation() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.toggleUserStatus(adminId, adminId, adminToken);
        });
    }
    
    @Test
    @DisplayName("✅ Actualizar usuario")
    void testUpdateUser_Success() {
        // Arrange
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .email("updated@example.com")
                .fullName("Updated Name")
                .build();
        
        when(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("updated@example.com"))
                .thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(jwtService.extractUserId(adminToken))
                .thenReturn(adminId);
        when(jwtService.extractUsername(adminToken))
                .thenReturn("admin");
        when(jwtService.extractRole(adminToken))
                .thenReturn("ADMINISTRADOR");
        
        // Act
        UserResponse response = userService.updateUser(testUserId, updateRequest, adminToken);
        
        // Assert
        assertNotNull(response);
        
        verify(auditClient, times(1)).registrarEvento(any(AuditEventDTO.class), anyString());
    }
    
    @Test
    @DisplayName("✅ Eliminar usuario")
    void testDeleteUser_Success() {
        // Arrange
        UUID otherAdminId = UUID.randomUUID();
        when(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser));
        when(jwtService.extractUserId(adminToken))
                .thenReturn(otherAdminId);
        when(jwtService.extractUsername(adminToken))
                .thenReturn("admin");
        when(jwtService.extractRole(adminToken))
                .thenReturn("ADMINISTRADOR");
        
        // Act
        userService.deleteUser(testUserId, adminToken);
        
        // Assert
        verify(userRepository, times(1)).delete(testUser);
        verify(auditClient, times(1)).registrarEvento(any(AuditEventDTO.class), anyString());
    }
    
    @Test
    @DisplayName("❌ No se puede auto-eliminar")
    void testDeleteUser_SelfDeletion() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(adminId, adminToken);
        });
    }
}
