package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.Center;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CenterRepository extends JpaRepository<Center, Long> {
    Optional<Center> findByName(String name);
}