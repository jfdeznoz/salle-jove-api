package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklySessionRepository extends JpaRepository<WeeklySession, Long> {

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
          AND ws.group.id = :groupId
          AND (
               (:isPast = true  AND CAST(ws.sessionDateTime AS date) <  :today)
            OR (:isPast = false AND CAST(ws.sessionDateTime AS date) >= :today)
          )
          AND (:onlyPublished = false OR ws.status = 1)
        ORDER BY ws.sessionDateTime ASC
    """)
    Page<WeeklySession> findByGroupIdAndPastStatus(@Param("groupId") Long groupId,
                                                   @Param("isPast") boolean isPast,
                                                   @Param("today") LocalDate today,
                                                   @Param("onlyPublished") boolean onlyPublished,
                                                   Pageable pageable);

    @Query("SELECT ws FROM WeeklySession ws WHERE ws.id = :id AND ws.deletedAt IS NULL")
    Optional<WeeklySession> findById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE WeeklySession ws SET ws.deletedAt = CURRENT_TIMESTAMP WHERE ws.id = :sessionId")
    void softDeleteSession(@Param("sessionId") Long sessionId);

    @Query("""
        SELECT ws
        FROM WeeklySession ws
        WHERE ws.deletedAt IS NULL
          AND ws.status = 1
          AND CAST(ws.sessionDateTime AS date) < :today
    """)
    List<WeeklySession> findPublishedSessionsBeforeDate(@Param("today") LocalDate today);
}

