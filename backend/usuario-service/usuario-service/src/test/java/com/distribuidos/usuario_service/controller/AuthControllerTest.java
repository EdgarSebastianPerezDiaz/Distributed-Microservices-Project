package com.distribuidos.usuario_service.controller;

import com.distribuidos.usuario_service.dto.*;
import com.distribuidos.usuario_service.exception.InvalidCredentialsException;
import com.distribuidos.usuario_service.exception.UserAlreadyExistsException;
import com.distribuidos.usuario_service.security.JwtService;
import com.distribuidos.usuario_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthController
 * 
 * @author Dev1 - Infraestructura
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {
    
    @Mock
    private UserService userService;
    
    @Mock
    private JwtService jwtService;
    
    @InjectMocks
    private AuthController authController;
    
    private UUID testUserId;
    private String testToken;
    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private UserRequest userRequest;
    private UserResponse userResponse;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testToken = "Bearer test-token-12345";
        
        // Setup LoginRequest
        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
        
        // Setup UserResponse
        userResponse = UserResponse.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .role("ADMINISTRADOR")
                .active(true)
                .createdAt(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .build();
        
        // Setup LoginResponse
        loginResponse = LoginResponse.builder()
                .token("test-jwt-token")
                .type("Bearer")
                .user(userResponse)
                .build();
        
        // Setup UserRequest
        userRequest = UserRequest.builder()
                .username("newuser")
                .password("SecurePass123!")
                .email("new@example.com")
                .fullName("New User")
                .role("FUNCIONARIO")
                .build();
    }
    
    @Test
    @DisplayName("✅ Login exitoso con credenciales válidas")
    void testLogin_Success() {
        // Arrange
        when(userService.login(any(LoginRequest.class)))
                .thenReturn(loginResponse);
        
        // Act
        ResponseEntity<LoginResponse> response = authController.login(loginRequest);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test-jwt-token", response.getBody().getToken());
        assertEquals("Bearer", response.getBody().getType());
        assertEquals("testuser", response.getBody().getUser().getUsername());
        
        verify(userService, times(1)).login(any(LoginRequest.class));
    }
    
    @Test
    @DisplayName("❌ Login falla con credenciales inválidas")
    void testLogin_InvalidCredentials() {
        // Arrange
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Usuario o contraseña incorrectos"));
        
        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authController.login(loginRequest);
        });
        
        verify(userService, times(1)).login(any(LoginRequest.class));
    }
    
    @Test
    @DisplayName("✅ Crear usuario (solo ADMIN)")
    void testRegister_Success() {
        // Arrange
        when(userService.createUser(any(UserRequest.class), anyString()))
                .thenReturn(userResponse);
        when(jwtService.extractUserId(anyString()))
                .thenReturn(testUserId);
        
        // Act
        ResponseEntity<UserResponse> response = authController.register(userRequest, testToken);
        
        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        
        verify(userService, times(1)).createUser(any(UserRequest.class), anyString());
    }
    
    @Test
    @DisplayName("❌ Crear usuario falla si username ya existe")
    void testRegister_UsernameAlreadyExists() {
        // Arrange
        when(userService.createUser(any(UserRequest.class), anyString()))
                .thenThrow(new UserAlreadyExistsException("El username ya existe"));
        
        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            authController.register(userRequest, testToken);
        });
    }
    
    @Test
    @DisplayName("✅ Obtener usuario actual (/auth/me)")
    void testGetCurrentUser_Success() {
        // Arrange
        when(jwtService.extractUserId(anyString()))
                .thenReturn(testUserId);
        when(userService.getCurrentUser(any(UUID.class)))
                .thenReturn(userResponse);
        
        // Act
        ResponseEntity<UserResponse> response = authController.getCurrentUser(testToken);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUserId, response.getBody().getId());
        
        verify(jwtService, times(1)).extractUserId(anyString());
        verify(userService, times(1)).getCurrentUser(any(UUID.class));
    }
    
    @Test
    @DisplayName("✅ Listar todos los usuarios (solo ADMIN)")
    void testGetAllUsers_Success() {
        // Arrange
        when(userService.getAllUsers())
                .thenReturn(java.util.List.of(userResponse));
        
        // Act
        ResponseEntity<java.util.List<UserResponse>> response = authController.getAllUsers();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        
        verify(userService, times(1)).getAllUsers();
    }
    
    @Test
    @DisplayName("✅ Obtener usuario por ID")
    void testGetUserById_Success() {
        // Arrange
        when(userService.getUserById(any(UUID.class)))
                .thenReturn(userResponse);
        
        // Act
        ResponseEntity<UserResponse> response = authController.getUserById(testUserId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUserId, response.getBody().getId());
        
        verify(userService, times(1)).getUserById(any(UUID.class));
    }
    
    @Test
    @DisplayName("✅ Cambiar estado de usuario (ADMINISTRADOR)")
    void testToggleStatus_Success() {
        // Arrange
        UserResponse inactiveUser = UserResponse.builder()
                .id(testUserId)
                .username("testuser")
                .active(false)
                .build();
        
        when(jwtService.extractUserId(anyString()))
                .thenReturn(UUID.randomUUID()); // Different from testUserId
        when(userService.toggleUserStatus(any(UUID.class), any(UUID.class), anyString()))
                .thenReturn(inactiveUser);
        
        // Act
        ResponseEntity<UserResponse> response = authController.toggleStatus(testUserId, testToken);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getActive());
        
        verify(userService, times(1)).toggleUserStatus(any(UUID.class), any(UUID.class), anyString());
    }
    
    @Test
    @DisplayName("✅ Actualizar usuario")
    void testUpdateUser_Success() {
        // Arrange
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .email("updated@example.com")
                .fullName("Updated Name")
                .build();
        
        UserResponse updatedUser = userResponse;
        updatedUser.setEmail("updated@example.com");
        updatedUser.setFullName("Updated Name");
        
        when(userService.updateUser(any(UUID.class), any(UserUpdateRequest.class), anyString()))
                .thenReturn(updatedUser);
        
        // Act
        ResponseEntity<UserResponse> response = authController.updateUser(testUserId, updateRequest, testToken);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        verify(userService, times(1)).updateUser(any(UUID.class), any(UserUpdateRequest.class), anyString());
    }
    
    @Test
    @DisplayName("✅ Eliminar usuario")
    void testDeleteUser_Success() {
        // Arrange
        doNothing().when(userService).deleteUser(any(UUID.class), anyString());
        
        // Act
        ResponseEntity<Void> response = authController.deleteUser(testUserId, testToken);
        
        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(userService, times(1)).deleteUser(any(UUID.class), anyString());
    }
}
