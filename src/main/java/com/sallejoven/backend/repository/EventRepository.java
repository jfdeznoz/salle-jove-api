package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.GroupSalle;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
    SELECT e
      FROM Event e
     WHERE e.deletedAt IS NULL
       AND e.isGeneral = true
       AND (
            (:isPast = true  AND e.endDate <  :today)
         OR (:isPast = false AND (e.endDate IS NULL OR e.endDate >= :today))
       )
     ORDER BY e.eventDate ASC
""")
    Page<Event> findGeneralEvents(@Param("isPast") boolean isPast,
                                  @Param("today")  LocalDate today,
                                  Pageable pageable);

    /**
     * 2) Sólo locales de los grupos dados
     */
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
                                                @Param("isPast")  boolean isPast,
                                                @Param("today")   LocalDate today,
                                                Pageable pageable);

    /**
     * 3) Unión de generales + locales (para isGeneral == null en NO-admin)
     */
    @Query("""
    SELECT DISTINCT e
      FROM Event e
      LEFT JOIN EventGroup eg
        ON eg.event = e
       AND eg.deletedAt IS NULL
     WHERE e.deletedAt IS NULL
       AND ( e.isGeneral = true OR eg.groupSalle IN :groups )
       AND (
            (:isPast = true  AND e.endDate <  :today)
         OR (:isPast = false AND (e.endDate IS NULL OR e.endDate >= :today))
       )
     ORDER BY e.eventDate ASC
""")
    Page<Event> findGeneralOrUserLocalEvents(@Param("groups") List<GroupSalle> groups,
                                             @Param("isPast")  boolean isPast,
                                             @Param("today")   LocalDate today,
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

    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Event> findById(@Param("id") Long id);

    @Query("SELECT e FROM Event e WHERE e.deletedAt IS NULL")
    Page<Event> findAll(Pageable pageable);

    @Modifying
    @Query("UPDATE Event e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = :eventId")
    void softDeleteEvent(@Param("eventId") Long eventId);
}