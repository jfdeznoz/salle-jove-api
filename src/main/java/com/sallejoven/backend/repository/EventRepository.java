package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findAll(Pageable pageable);

    @Modifying
    @Query("UPDATE Event e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = :eventId")
    void softDeleteEvent(@Param("eventId") Long eventId);
}