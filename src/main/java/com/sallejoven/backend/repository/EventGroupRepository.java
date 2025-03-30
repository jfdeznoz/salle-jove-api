package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.EventGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventGroupRepository extends JpaRepository<EventGroup, Long> {

    List<EventGroup> findByEventId(Long eventId);
    List<EventGroup> findByEventIdAndGroupSalleIdIn(Long eventId, List<Long> groupIds);
    void deleteByEventIdAndGroupSalleIdIn(Long eventId, List<Long> groupSalleIds);
}