package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.Center;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CenterRepository extends JpaRepository<Center, UUID> {
    Optional<Center> findByName(String name);

    default Optional<Center> findByUuid(UUID uuid) {
        return findById(uuid);
    }

    Optional<Center> findByNameAndCity(String name, String city);

    boolean existsByNameAndCity(String name, String city);

    boolean existsByNameAndCityAndUuidNot(String name, String city, UUID uuid);
}
