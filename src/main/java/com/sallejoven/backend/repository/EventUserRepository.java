package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.EventUser;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventUserRepository extends JpaRepository<EventUser, Long> {

    @Query("""
        SELECT eu
        FROM EventUser eu
        JOIN eu.userGroup ug
        JOIN ug.group g
        JOIN ug.user u
        WHERE eu.event.id = :eventId
          AND g.id = :groupId
          AND eu.deletedAt IS NULL
          AND u.deletedAt IS NULL
    """)
    List<EventUser> findByEventIdAndGroupId(@Param("eventId") Integer eventId,
                                            @Param("groupId") Integer groupId);

    @Query("""
        SELECT DISTINCT eu
        FROM EventUser eu
        JOIN eu.userGroup ug
        JOIN ug.user u
        JOIN ug.group g
        WHERE eu.event.id  = :eventId
          AND eu.deletedAt IS NULL
          AND u.deletedAt  IS NULL
          AND eu.status    = 1
        ORDER BY g.center.name ASC, g.stage ASC
    """)
    List<EventUser> findConfirmedByEventIdOrdered(@Param("eventId") Long eventId);

    @Query("""
        SELECT eu
        FROM EventUser eu
        JOIN eu.userGroup ug
        JOIN ug.user u
        JOIN ug.group g
        WHERE eu.event.id  = :eventId
          AND eu.deletedAt IS NULL
          AND u.deletedAt  IS NULL
        ORDER BY g.center.name ASC, g.stage ASC
    """)
    List<EventUser> findByEventIdOrdered(@Param("eventId") Long eventId);

    @Query("""
        SELECT eu
        FROM EventUser eu
        WHERE eu.id = :id
          AND eu.deletedAt IS NULL
    """)
    Optional<EventUser> findById(@Param("id") Long id);

    @Modifying
    @Query("""
        UPDATE EventUser eu
           SET eu.deletedAt = CURRENT_TIMESTAMP
         WHERE eu.event.id = :eventId
           AND eu.deletedAt IS NULL
    """)
    void softDeleteByEventId(@Param("eventId") Long eventId);

    @Query("""
        SELECT eu.userGroup.id
        FROM EventUser eu
        WHERE eu.event.id = :eventId
    """)
    List<Long> findUserGroupIdsByEvent(@Param("eventId") Long eventId);

    @Modifying
    @Query("""
        UPDATE EventUser eu
           SET eu.status = :status
         WHERE eu.event.id = :eventId
           AND eu.userGroup.id IN (
                SELECT ug.id
                FROM UserGroup ug
                WHERE ug.user.id  = :userId
                  AND ug.group.id = :groupId
           )
           AND eu.deletedAt IS NULL
    """)
    int updateStatusByEventUserAndGroup(@Param("eventId") Long eventId,
                                        @Param("userId") Long userId,
                                        @Param("groupId") Long groupId,
                                        @Param("status") int status);

    @Modifying
    @Query("""
        UPDATE EventUser eu
           SET eu.deletedAt = CURRENT_TIMESTAMP
         WHERE eu.event.id    = :eventId
           AND eu.userGroup.id IN :userGroupIds
           AND eu.deletedAt    IS NULL
    """)
    int softDeleteByEventIdAndUserGroupIds(@Param("eventId") Long eventId,
                                           @Param("userGroupIds") Collection<Long> userGroupIds);

    @Modifying
    @Query("UPDATE EventUser eu SET eu.deletedAt = :when WHERE eu.userGroup.id IN :ugIds AND eu.deletedAt IS NULL")
    int softDeleteByUserGroupIdIn(@Param("ugIds") List<Long> ugIds, @Param("when") LocalDateTime when);
}