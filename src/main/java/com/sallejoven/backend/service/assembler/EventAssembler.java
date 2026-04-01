package com.sallejoven.backend.service.assembler;

import com.sallejoven.backend.mapper.EventMapper;
import com.sallejoven.backend.model.dto.EventDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.service.EventGroupService;
import com.sallejoven.backend.repository.EventGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        EventGroup eg = eventGroupService.findFirstActiveByEventId(event.getId());
        if (eg == null || eg.getGroupSalle() == null) {
            return eventMapper.toEventDto(event, null, null);
        }
        Center c = eg.getGroupSalle().getCenter();
        return eventMapper.toEventDto(event, c.getId(), formatCenterName(c));
    }

    public Page<EventDto> toDtoPage(Page<Event> events) {
        List<Long> nonGeneralIds = events.getContent().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsGeneral()))
                .map(Event::getId)
                .toList();

        Map<Long, Center> centerByEventId = batchResolveCenters(nonGeneralIds);

        return events.map(event -> {
            if (Boolean.TRUE.equals(event.getIsGeneral())) {
                return eventMapper.toEventDto(event, null, null);
            }
            Center c = centerByEventId.get(event.getId());
            return eventMapper.toEventDto(event,
                    c != null ? c.getId() : null,
                    c != null ? formatCenterName(c) : null);
        });
    }

    private Map<Long, Center> batchResolveCenters(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<EventGroup> egs = eventGroupRepository.findActiveByEventIds(eventIds);

        Map<Long, Center> result = new LinkedHashMap<>();
        for (EventGroup eg : egs) {
            Long eventId = eg.getEvent().getId();
            if (!result.containsKey(eventId) && eg.getGroupSalle() != null) {
                result.put(eventId, eg.getGroupSalle().getCenter());
            }
        }
        return result;
    }

    private String formatCenterName(Center c) {
        String name = c.getName() != null ? c.getName() : "";
        String city = c.getCity();
        return (city != null && !city.isBlank()) ? name + " (" + city + ")" : name;
    }
}
