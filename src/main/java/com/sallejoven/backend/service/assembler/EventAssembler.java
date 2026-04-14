package com.sallejoven.backend.service.assembler;

import com.sallejoven.backend.mapper.EventMapper;
import com.sallejoven.backend.model.dto.EventDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.repository.EventGroupRepository;
import com.sallejoven.backend.service.EventGroupService;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventAssembler {

    private final EventGroupRepository eventGroupRepository;
    private final EventGroupService eventGroupService;
    private final EventMapper eventMapper;

    public EventDto toDto(Event event) {
        if (Boolean.TRUE.equals(event.getIsGeneral())) {
            return eventMapper.toEventDto(event, null, null);
        }
        EventGroup eventGroup = eventGroupService.findFirstActiveByEventId(event.getUuid());
        if (eventGroup == null || eventGroup.getGroupSalle() == null) {
            return eventMapper.toEventDto(event, null, null);
        }
        Center center = eventGroup.getGroupSalle().getCenter();
        return eventMapper.toEventDto(event, center != null ? center.getUuid() : null, center != null ? formatCenterName(center) : null);
    }

    public Page<EventDto> toDtoPage(Page<Event> events) {
        List<UUID> nonGeneralUuids = events.getContent().stream()
                .filter(event -> !Boolean.TRUE.equals(event.getIsGeneral()))
                .map(Event::getUuid)
                .toList();

        Map<UUID, Center> centerByEventUuid = batchResolveCenters(nonGeneralUuids);
        return events.map(event -> {
            if (Boolean.TRUE.equals(event.getIsGeneral())) {
                return eventMapper.toEventDto(event, null, null);
            }
            Center center = centerByEventUuid.get(event.getUuid());
            return eventMapper.toEventDto(event,
                    center != null ? center.getUuid() : null,
                    center != null ? formatCenterName(center) : null);
        });
    }

    private Map<UUID, Center> batchResolveCenters(Collection<UUID> eventUuids) {
        if (eventUuids.isEmpty()) {
            return Map.of();
        }

        List<EventGroup> eventGroups = eventGroupRepository.findActiveByEventUuids(eventUuids);
        Map<UUID, Center> result = new LinkedHashMap<>();
        for (EventGroup eventGroup : eventGroups) {
            UUID eventUuid = eventGroup.getEvent().getUuid();
            if (!result.containsKey(eventUuid) && eventGroup.getGroupSalle() != null) {
                result.put(eventUuid, eventGroup.getGroupSalle().getCenter());
            }
        }
        return result;
    }

    private String formatCenterName(Center center) {
        String name = center.getName() != null ? center.getName() : "";
        String city = center.getCity();
        return (city != null && !city.isBlank()) ? name + " (" + city + ")" : name;
    }
}
