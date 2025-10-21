package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserPending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPendingRepository extends JpaRepository<UserPending, Long> {
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
    Optional<UserPending> findTopById(Long id);

    List<UserPending> findByCenterIdIn(Collection<Long> centerIds);

    @Query(value = """
        SELECT up.*
        FROM user_pending up
        JOIN group_salle g ON g.id = up.group_id
        WHERE g.center IN (:centerIds)
        """, nativeQuery = true)
    List<UserPending> findByGroupCenterIds(@Param("centerIds") Collection<Long> centerIds);
}