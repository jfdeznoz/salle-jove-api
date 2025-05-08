package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.repository.EventGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventGroupService {

    private final EventGroupRepository eventGroupRepository;

    @Autowired
    public EventGroupService(EventGroupRepository eventGroupRepository) {
        this.eventGroupRepository = eventGroupRepository;
    }
    
    public List<EventGroup> saveAllEventGroup(List<EventGroup> eventGroups) {
        return eventGroupRepository.saveAll(eventGroups);
    }

    public List<EventGroup> getEventGroupsByEventId(Long eventId) {
        return eventGroupRepository.findByEventId(eventId);
    }    

    public List<EventGroup> getEventGroupsByEventIdAndGroupIds(Long eventId, List<Long> groupIds) {
        return eventGroupRepository.findByEventIdAndGroupSalleIdIn(eventId, groupIds);
    }

    public void deleteEventGroupsByEventAndGroups(Long eventId, List<GroupSalle> groups) {
        if (groups == null || groups.isEmpty()) return;

        List<Long> groupIds = groups.stream()
            .map(GroupSalle::getId)
            .toList();

        eventGroupRepository.deleteByEventIdAndGroupSalleIdIn(eventId, groupIds);
    }

    public void softDeleteByEventId(Long eventId) {
        eventGroupRepository.softDeleteByEventId(eventId);
    }

}