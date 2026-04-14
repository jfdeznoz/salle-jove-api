package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklySessionRepository extends JpaRepository<WeeklySession, UUID> {

    default Optional<WeeklySession> findByUuid(UUID uuid) {
        return findById(uuid);
    }

    @Query("""
        SELECT ws
        FROM WeeklySession ws
        WHERE ws.deletedAt IS NULL
          AND ws.group IN :groups
          AND (
               (:isPast = true  AND CAST(ws.sessionDateTime AS date) <  :today)
            OR (:isPast = false AND CAST(ws.sessionDateTime AS date) >= :today)
          )
          AND (:onlyPublished = false OR ws.status = 1)
        ORDER BY ws.sessionDateTime ASC
    """)
    Page<WeeklySession> findByGroupsAndPastStatus(@Param("groups") List<GroupSalle> groups,
                                                  @Param("isPast") boolean isPast,
                                                  @Param("today") LocalDate today,
                                                  @Param("onlyPublished") boolean onlyPublished,
                                                  Pageable pageable);

    @Query("""
        SELECT ws
        FROM WeeklySession ws
        WHERE ws.deletedAt IS NULL
          AND ws.group.uuid = :groupUuid
          AND (
               (:isPast = true  AND CAST(ws.sessionDateTime AS date) <  :today)
            OR (:isPast = false AND CAST(ws.sessionDateTime AS date) >= :today)
          )
          AND (:onlyPublished = false OR ws.status = 1)
        ORDER BY ws.sessionDateTime ASC
    """)
    Page<WeeklySession> findByGroupUuidAndPastStatus(@Param("groupUuid") UUID groupUuid,
                                                     @Param("isPast") boolean isPast,
                                                     @Param("today") LocalDate today,
                                                     @Param("onlyPublished") boolean onlyPublished,
                                                     Pageable pageable);

    @Query("SELECT ws FROM WeeklySession ws WHERE ws.uuid = :uuid AND ws.deletedAt IS NULL")
    Optional<WeeklySession> findById(@Param("uuid") UUID uuid);

    interface AuthView {
        UUID getUuid();
        Integer getStatus();
        UUID getGroupUuid();
        UUID getCenterUuid();
    }

    @Query("""
        SELECT ws.uuid AS uuid,
               ws.status AS status,
               ws.group.uuid AS groupUuid,
               ws.group.center.uuid AS centerUuid
        FROM WeeklySession ws
        WHERE ws.uuid = :uuid AND ws.deletedAt IS NULL
    """)
    Optional<AuthView> findAuthViewByUuid(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE WeeklySession ws SET ws.deletedAt = CURRENT_TIMESTAMP WHERE ws.uuid = :sessionUuid")
    void softDeleteSession(@Param("sessionUuid") UUID sessionUuid);

    @Query("""
        SELECT ws
        FROM WeeklySession ws
        WHERE ws.deletedAt IS NULL
          AND ws.status = 1
          AND CAST(ws.sessionDateTime AS date) < :today
    """)
    List<WeeklySession> findPublishedSessionsBeforeDate(@Param("today") LocalDate today);

    @Query("""
        SELECT ws
        FROM WeeklySession ws
        WHERE ws.deletedAt IS NULL
          AND ws.group IN :groups
          AND CAST(ws.sessionDateTime AS date) = :sessionDate
          AND (:onlyPublished = false OR ws.status = 1)
        ORDER BY ws.sessionDateTime ASC
    """)
    Page<WeeklySession> findByGroupsAndSessionDate(@Param("groups") List<GroupSalle> groups,
                                                   @Param("sessionDate") LocalDate sessionDate,
                                                   @Param("onlyPublished") boolean onlyPublished,
                                                   Pageable pageable);
}
