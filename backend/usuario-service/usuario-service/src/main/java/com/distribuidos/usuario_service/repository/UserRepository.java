package com.distribuidos.usuario_service.repository;

import com.distribuidos.usuario_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    // Buscar por username (para login)
    Optional<User> findByUsername(String username);
    
    // Buscar por email
    Optional<User> findByEmail(String email);
    
    // Verificar si existe username
    boolean existsByUsername(String username);
    
    // Verificar si existe email
    boolean existsByEmail(String email);
    
    // Buscar usuarios activos
    List<User> findByActiveTrue();
    
    // Buscar por rol
    List<User> findByRole_Name(String roleName);
}