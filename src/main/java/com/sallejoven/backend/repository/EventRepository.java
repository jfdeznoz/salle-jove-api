package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.GroupSalle;
import java.time.LocalDate;
import java.util.Date;
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
public interface EventRepository extends JpaRepository<Event, UUID> {

    default Optional<Event> findByUuid(UUID uuid) {
        return findById(uuid);
    }

    @Query(value = """
    SELECT e.*
      FROM event e
     WHERE e.deleted_at IS NULL
       AND e.is_general = true
       AND e.stages && CAST(:userStages AS integer[])
       AND (
            (:isPast = true  AND e.end_date <  :today)
         OR (:isPast = false AND (e.end_date IS NULL OR e.end_date >= :today))
       )
     ORDER BY e.event_date ASC
""", countQuery = """
    SELECT COUNT(*)
      FROM event e
     WHERE e.deleted_at IS NULL
       AND e.is_general = true
       AND e.stages && CAST(:userStages AS integer[])
       AND (
            (:isPast = true  AND e.end_date <  :today)
         OR (:isPast = false AND (e.end_date IS NULL OR e.end_date >= :today))
       )
""", nativeQuery = true)
    Page<Event> findGeneralEvents(@Param("userStages") Integer[] userStages,
                                  @Param("isPast") boolean isPast,
                                  @Param("today") LocalDate today,
                                  Pageable pageable);

    @Query("""
    SELECT eg.event
      FROM EventGroup eg
     WHERE eg.deletedAt        IS NULL
       AND eg.groupSalle      IN :groups
       AND eg.event.deletedAt IS NULL
       AND eg.event.isGeneral = false
       AND (
            (:isPast = true  AND eg.event.endDate <  :today)
         OR (:isPast = false AND (eg.event.endDate IS NULL OR eg.event.endDate >= :today))
       )
     ORDER BY eg.event.eventDate ASC
""")
    Page<Event> findEventsByGroupsAndPastStatus(@Param("groups") List<GroupSalle> groups,
                                                @Param("isPast") boolean isPast,
                                                @Param("today") LocalDate today,
                                                Pageable pageable);

    @Query(value = """
    SELECT DISTINCT e.*
      FROM event e
      LEFT JOIN event_group eg
        ON eg.event_uuid = e.uuid
       AND eg.deleted_at IS NULL
     WHERE e.deleted_at IS NULL
       AND (
            (e.is_general = true AND e.stages && CAST(:userStages AS integer[]))
         OR (e.is_general = false AND eg.group_uuid = ANY(CAST(:groupUuids AS uuid[])))
       )
       AND (
            (:isPast = true  AND e.end_date <  :today)
         OR (:isPast = false AND (e.end_date IS NULL OR e.end_date >= :today))
       )
     ORDER BY e.event_date ASC
""", countQuery = """
    SELECT COUNT(DISTINCT e.uuid)
      FROM event e
      LEFT JOIN event_group eg
        ON eg.event_uuid = e.uuid
       AND eg.deleted_at IS NULL
     WHERE e.deleted_at IS NULL
       AND (
            (e.is_general = true AND e.stages && CAST(:userStages AS integer[]))
         OR (e.is_general = false AND eg.group_uuid = ANY(CAST(:groupUuids AS uuid[])))
       )
       AND (
            (:isPast = true  AND e.end_date <  :today)
         OR (:isPast = false AND (e.end_date IS NULL OR e.end_date >= :today))
       )
""", nativeQuery = true)
    Page<Event> findGeneralOrUserLocalEvents(@Param("groupUuids") UUID[] groupUuids,
                                             @Param("userStages") Integer[] userStages,
                                             @Param("isPast") boolean isPast,
                                             @Param("today") LocalDate today,
                                             Pageable pageable);

    @Query("""
    SELECT e FROM Event e
     WHERE e.deletedAt IS NULL
       AND (:isGeneral IS NULL OR e.isGeneral = :isGeneral)
       AND (
           (:isPast = true  AND e.endDate IS NOT NULL AND e.endDate < :now)
        OR (:isPast = false AND (e.endDate IS NULL OR e.endDate >= :now))
       )
     ORDER BY e.eventDate ASC
""")
    Page<Event> findAdminFilteredEvents(@Param("isGeneral") Boolean isGeneral,
                                        @Param("isPast") boolean isPast,
                                        @Param("now") LocalDate now,
                                        Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.endDate IS NOT NULL AND e.endDate < :now AND e.deletedAt IS NULL")
    Page<Event> findPastEvents(@Param("now") Date now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE (e.endDate IS NULL OR e.endDate >= :now) AND e.deletedAt IS NULL")
    Page<Event> findFutureEvents(@Param("now") Date now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.uuid = :uuid AND e.deletedAt IS NULL")
    Optional<Event> findById(@Param("uuid") UUID uuid);

    @Query("SELECT e FROM Event e WHERE e.deletedAt IS NULL")
    Page<Event> findAll(Pageable pageable);

    @Modifying
    @Query("UPDATE Event e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.uuid = :eventUuid")
    void softDeleteEvent(@Param("eventUuid") UUID eventUuid);

    @Query("""
    SELECT e FROM Event e
     WHERE e.deletedAt IS NULL
       AND e.eventDate <= :endDate
       AND COALESCE(e.endDate, e.eventDate) >= :startDate
       AND (:isGeneral IS NULL OR e.isGeneral = :isGeneral)
     ORDER BY e.eventDate ASC
""")
    Page<Event> findAdminByDateRange(@Param("isGeneral") Boolean isGeneral,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     Pageable pageable);

    @Query(value = """
    SELECT DISTINCT e.*
      FROM event e
      LEFT JOIN event_group eg
       ON eg.event_uuid = e.uuid
       AND eg.deleted_at IS NULL
     WHERE e.deleted_at IS NULL
       AND e.event_date <= :endDate
       AND COALESCE(e.end_date, e.event_date) >= :startDate
       AND (:isGeneral IS NULL OR e.is_general = :isGeneral)
       AND (
            (e.is_general = true AND e.stages && CAST(:userStages AS integer[]))
         OR (e.is_general = false AND eg.group_uuid = ANY(CAST(:groupUuids AS uuid[])))
       )
     ORDER BY e.event_date ASC
""", countQuery = """
    SELECT COUNT(DISTINCT e.uuid)
      FROM event e
      LEFT JOIN event_group eg
        ON eg.event_uuid = e.uuid
       AND eg.deleted_at IS NULL
     WHERE e.deleted_at IS NULL
       AND e.event_date <= :endDate
       AND COALESCE(e.end_date, e.event_date) >= :startDate
       AND (:isGeneral IS NULL OR e.is_general = :isGeneral)
       AND (
            (e.is_general = true AND e.stages && CAST(:userStages AS integer[]))
         OR (e.is_general = false AND eg.group_uuid = ANY(CAST(:groupUuids AS uuid[])))
       )
""", nativeQuery = true)
    Page<Event> findByDateRangeAndGroups(@Param("groupUuids") UUID[] groupUuids,
                                         @Param("userStages") Integer[] userStages,
                                         @Param("isGeneral") Boolean isGeneral,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         Pageable pageable);
}
