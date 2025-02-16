package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Buscar un usuario por email (para autenticación y otras operaciones)
    public Optional<UserSalle> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Crear o actualizar un usuario
    public UserSalle saveUser(UserSalle user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    // Buscar todos los usuarios
    public List<UserSalle> findAllUsers() {
        return userRepository.findAll();
    }

    // Comprobar si un usuario existe por DNI o email
    public boolean existsByDni(String dni) {
        return userRepository.existsByDni(dni);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Buscar un usuario por ID
    public Optional<UserSalle> findById(Long id) {
        return userRepository.findById(id);
    }

    // Eliminar un usuario por ID
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /*@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserSalle user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Convertir los roles del usuario en GrantedAuthority para Spring Security
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }*/
}