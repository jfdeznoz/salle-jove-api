package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.ids.EventUserId;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventUserRepository extends JpaRepository<EventUser, EventUserId> {

    @Query("""
        SELECT eu FROM EventUser eu
        JOIN eu.user u
        JOIN u.groups g
        WHERE eu.event.id = :eventId
        AND g.id = :groupId
        AND eu.deletedAt IS NULL
        AND u.deletedAt IS NULL
    """)
    List<EventUser> findByEventIdAndGroupId(@Param("eventId") Integer eventId, @Param("groupId") Integer groupId);

    @Query("""
        SELECT eu FROM EventUser eu
        JOIN FETCH eu.user u
        JOIN u.groups g
        WHERE eu.event.id    = :eventId
        AND eu.deletedAt   IS NULL
        AND u.deletedAt    IS NULL
        AND eu.status      = 1
        ORDER BY g.center.name ASC, g.stage ASC
    """)
    List<EventUser> findConfirmedByEventIdOrdered(@Param("eventId") Long eventId);

    @Query("""
        SELECT eu FROM EventUser eu
        JOIN FETCH eu.user u
        JOIN u.groups g
        WHERE eu.event.id = :eventId
        AND eu.deletedAt IS NULL
        AND u.deletedAt IS NULL
        ORDER BY g.center.name ASC, g.stage ASC
    """)
    List<EventUser> findByEventIdOrdered(@Param("eventId") Long eventId);


    @Query("SELECT eu FROM EventUser eu WHERE eu.id = :id AND eu.deletedAt IS NULL")
    Optional<EventUser> findById(@Param("id") EventUserId id);

    @Modifying
    @Query("UPDATE EventUser eu SET eu.deletedAt = CURRENT_TIMESTAMP WHERE eu.event.id = :eventId")
    void softDeleteByEventId(@Param("eventId") Long eventId);
}