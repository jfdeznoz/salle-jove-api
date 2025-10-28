package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.repository.EventGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@RequiredArgsConstructor
@Service
public class EventGroupService {

    private final EventGroupRepository eventGroupRepository;

    public EventGroup findFirstActiveByEventId(Long eventId) {
        return eventGroupRepository
                .findFirstByEvent_IdAndDeletedAtIsNullOrderByIdAsc(eventId)
                .orElse(null);
    }

    public List<EventGroup> saveAllEventGroup(List<EventGroup> eventGroups) {
        return eventGroupRepository.saveAll(eventGroups);
    }

    public List<EventGroup> getEventGroupsByEventId(Long eventId) {
        return eventGroupRepository.findByEventId(eventId);
    }    

    public List<EventGroup> getEventGroupsByEventIdAndGroupIds(Long eventId, List<Long> groupIds) {
        return eventGroupRepository.findByEvent_IdAndGroupSalle_IdIn(eventId, groupIds);
    }

    public List<EventGroup> getEventGroupsByGroupId(Long groupId) {
        return eventGroupRepository.findByGroupSalle_Id(groupId);
    }

    public void deleteEventGroupsByEventAndGroups(Long eventId, List<GroupSalle> groups) {
        if (groups == null || groups.isEmpty()) return;

        List<Long> groupIds = groups.stream()
            .map(GroupSalle::getId)
            .toList();

        eventGroupRepository.deleteByEvent_IdAndGroupSalle_IdIn(eventId, groupIds);
    }

    public void softDeleteByEventId(Long eventId) {
        eventGroupRepository.softDeleteByEventId(eventId);
    }

    public List<EventGroup> getEventGroupsByEventAndCenter(Long eventId, Long centerId) {
        return eventGroupRepository.findByEventIdAndCenterId(eventId, centerId);
    }

    public List<EventGroup> getEventGroupsByEventAndCenters(Long eventId, List<Long> centerIds) {
        if (centerIds == null || centerIds.isEmpty()) return List.of();
        return eventGroupRepository.findByEventIdAndCenterIds(eventId, centerIds);
    }

    /*@Transactional
    public void assignEventToUserGroups(Long eventId, Collection<Long> userGroupIds) {
        if (userGroupIds == null || userGroupIds.isEmpty()) return;

        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado ID: " + eventId));

        List<EventUser> batch = userGroupIds.stream()
                .map(ugId -> EventUser.builder()
                        .event(event)
                        .userGroup(UserGroup.builder().id(ugId).build())
                        .status(0)
                        .build())
                .toList();

        try {
            eventUserService.saveAll(batch);
        } catch (DataIntegrityViolationException dup) {
            log.debug("Asignaciones duplicadas para eventId={}", eventId, dup);
        }
    }*/

}