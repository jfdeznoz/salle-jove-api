package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.UserPending;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPendingRepository extends JpaRepository<UserPending, UUID> {
    boolean existsByEmail(String email);

    boolean existsByDni(String dni);

    Optional<UserPending> findTopByUuid(UUID uuid);

    default Optional<UserPending> findByUuid(UUID uuid) {
        return findById(uuid);
    }

    List<UserPending> findByCenterUuidIn(Collection<UUID> centerUuids);

    @Query(value = """
        SELECT up.*
        FROM user_pending up
        JOIN group_salle g ON g.uuid = up.group_uuid
        WHERE g.center_uuid IN (:centerUuids)
        """, nativeQuery = true)
    List<UserPending> findByGroupCenterUuids(@Param("centerUuids") Collection<UUID> centerUuids);
}
