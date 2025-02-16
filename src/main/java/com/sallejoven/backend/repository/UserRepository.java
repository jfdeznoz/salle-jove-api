package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserSalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserSalle, Long> {

    Optional<UserSalle> findByEmail(String email);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);
}