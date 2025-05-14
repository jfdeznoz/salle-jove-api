package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.EventGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventGroupRepository extends JpaRepository<EventGroup, Long> {

    @Query("SELECT eg FROM EventGroup eg WHERE eg.id.event = :eventId AND eg.deletedAt IS NULL")
    List<EventGroup> findByEventId(@Param("eventId") Long eventId);

    List<EventGroup> findByEventIdAndGroupSalleIdIn(Long eventId, List<Long> groupIds);
    void deleteByEventIdAndGroupSalleIdIn(Long eventId, List<Long> groupSalleIds);

    @Modifying
    @Query("UPDATE EventGroup eg SET eg.deletedAt = CURRENT_TIMESTAMP WHERE eg.event.id = :eventId")
    void softDeleteByEventId(@Param("eventId") Long eventId);
}