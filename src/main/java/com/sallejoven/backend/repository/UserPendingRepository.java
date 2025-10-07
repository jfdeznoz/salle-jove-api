package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserPending;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPendingRepository extends JpaRepository<UserPending, Long> {
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
    Optional<UserPending> findTopById(Long id);
}