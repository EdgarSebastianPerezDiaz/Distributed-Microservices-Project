package com.distribuidos.usuario_service.security;

import com.distribuidos.usuario_service.model.User;
import com.distribuidos.usuario_service.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Servicio para cargar detalles del usuario desde la base de datos
 * Implementa UserDetailsService para integración con Spring Security
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

		if (!user.getActive()) {
			throw new UsernameNotFoundException("Usuario desactivado: " + username);
		}

		// Convertir rol de BD a GrantedAuthority para Spring Security
		SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
			"ROLE_" + user.getRole().getName()
		);

		return org.springframework.security.core.userdetails.User.builder()
			.username(user.getUsername())
			.password(user.getPasswordHash())
			.authorities(Collections.singleton(authority))
			.accountExpired(false)
			.accountLocked(false)
			.credentialsExpired(false)
			.disabled(!user.getActive())
			.build();
	}
}
