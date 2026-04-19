package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.repository.projection.UserLedSessionProjection;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        LocalDateTime getSessionDateTime();
    }

    @Query("""
        SELECT ws.uuid AS uuid,
               ws.status AS status,
               ws.group.uuid AS groupUuid,
               ws.group.center.uuid AS centerUuid,
               ws.sessionDateTime AS sessionDateTime
        FROM WeeklySession ws
        WHERE ws.uuid = :uuid AND ws.deletedAt IS NULL
    """)
    Optional<AuthView> findAuthViewByUuid(@Param("uuid") UUID uuid);

    interface WeeklySessionSummaryProjection {
        UUID getGroupUuid();
        Long getCurrentWeekCount();
        Long getPreviousWeekCount();
    }

    @Query("""
        SELECT ws.group.uuid AS groupUuid,
               COALESCE(SUM(CASE
                   WHEN CAST(ws.sessionDateTime AS date) BETWEEN :currentWeekStart AND :currentWeekEnd
                   THEN 1 ELSE 0 END), 0) AS currentWeekCount,
               COALESCE(SUM(CASE
                   WHEN CAST(ws.sessionDateTime AS date) BETWEEN :previousWeekStart AND :previousWeekEnd
                   THEN 1 ELSE 0 END), 0) AS previousWeekCount
        FROM WeeklySession ws
        WHERE ws.deletedAt IS NULL
          AND ws.group.uuid IN :groupUuids
          AND CAST(ws.sessionDateTime AS date) BETWEEN :previousWeekStart AND :currentWeekEnd
        GROUP BY ws.group.uuid
    """)
    List<WeeklySessionSummaryProjection> summarizeWeeklySessionsByGroup(
            @Param("groupUuids") List<UUID> groupUuids,
            @Param("currentWeekStart") LocalDate currentWeekStart,
            @Param("currentWeekEnd") LocalDate currentWeekEnd,
            @Param("previousWeekStart") LocalDate previousWeekStart,
            @Param("previousWeekEnd") LocalDate previousWeekEnd);

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

    @Query("""
        SELECT DISTINCT ws.uuid AS sessionUuid,
               ws.sessionDateTime AS date,
               ws.title AS title,
               vs.title AS vitalSituationTitle,
               vss.title AS vitalSituationSessionTitle,
               ws.content AS content,
               ws.observations AS observations,
               ws.group.center.name AS centerName,
               ws.group.stage AS stage
        FROM WeeklySession ws
        JOIN ws.vitalSituationSession vss
        JOIN vss.vitalSituation vs
        JOIN UserGroup ug
          ON ug.group.uuid = ws.group.uuid
        WHERE ug.user.uuid = :userUuid
          AND ug.userType = 1
          AND ug.year = :year
          AND ug.deletedAt IS NULL
          AND ws.deletedAt IS NULL
          AND ws.sessionDateTime >= :academicYearStart
          AND ws.sessionDateTime < :nextAcademicYearStart
        ORDER BY ws.sessionDateTime DESC, ws.uuid DESC
    """)
    List<UserLedSessionProjection> findLedSessionsByUser(@Param("userUuid") UUID userUuid,
                                                         @Param("year") Integer year,
                                                         @Param("academicYearStart") LocalDateTime academicYearStart,
                                                         @Param("nextAcademicYearStart") LocalDateTime nextAcademicYearStart,
                                                         Pageable pageable);
}
