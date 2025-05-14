package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.Event;
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
    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Event> findById(@Param("id") Long id);

    @Query("SELECT e FROM Event e WHERE e.deletedAt IS NULL")
    Page<Event> findAll(Pageable pageable);

    @Modifying
    @Query("UPDATE Event e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = :eventId")
    void softDeleteEvent(@Param("eventId") Long eventId);
}