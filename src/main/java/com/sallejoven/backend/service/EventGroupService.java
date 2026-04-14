package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.repository.EventGroupRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EventGroupService {

    private final EventGroupRepository eventGroupRepository;

    public EventGroup findFirstActiveByEventId(UUID eventUuid) {
        return eventGroupRepository
                .findFirstByEvent_UuidAndDeletedAtIsNullOrderByUuidAsc(eventUuid)
                .orElse(null);
    }

    public List<EventGroup> saveAllEventGroup(List<EventGroup> eventGroups) {
        return eventGroupRepository.saveAll(eventGroups);
    }

    public List<EventGroup> getEventGroupsByEventId(UUID eventUuid) {
        return eventGroupRepository.findByEventUuid(eventUuid);
    }

    public List<EventGroup> getEventGroupsByEventIdAndGroupIds(UUID eventUuid, List<UUID> groupUuids) {
        return eventGroupRepository.findByEvent_UuidAndGroupSalle_UuidIn(eventUuid, groupUuids);
    }

    public List<EventGroup> getEventGroupsByGroupId(UUID groupUuid) {
        return eventGroupRepository.findByGroupSalle_Uuid(groupUuid);
    }

    public void deleteEventGroupsByEventAndGroups(UUID eventUuid, List<GroupSalle> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }

        List<UUID> groupUuids = groups.stream()
                .map(GroupSalle::getUuid)
                .toList();

        eventGroupRepository.deleteByEvent_UuidAndGroupSalle_UuidIn(eventUuid, groupUuids);
    }

    public void softDeleteByEventId(UUID eventUuid) {
        eventGroupRepository.softDeleteByEventUuid(eventUuid);
    }

    public List<EventGroup> getEventGroupsByEventAndCenter(UUID eventUuid, UUID centerUuid) {
        return eventGroupRepository.findByEventUuidAndCenterUuid(eventUuid, centerUuid);
    }

    public List<EventGroup> getEventGroupsByEventAndCenters(UUID eventUuid, List<UUID> centerUuids) {
        if (centerUuids == null || centerUuids.isEmpty()) {
            return List.of();
        }
        return eventGroupRepository.findByEventUuidAndCenterUuids(eventUuid, centerUuids);
    }
}
