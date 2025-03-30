package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.ids.EventGroupId;
import com.sallejoven.backend.model.ids.EventUserId;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.model.requestDto.RequestEvent;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.EventGroupRepository;
import com.sallejoven.backend.repository.EventRepository;
import com.sallejoven.backend.repository.EventUserRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventGroupService eventGroupService;
    private final EventUserRepository eventUserRepository;
    private final GroupService groupService;
    private final UserService userService;

    @Autowired
    public EventService(EventRepository eventRepository, GroupService groupService, EventGroupService eventGroupService, EventUserRepository eventUserRepository, UserService userService) {
        this.eventRepository = eventRepository;
        this.groupService = groupService;
        this.eventGroupService = eventGroupService;
        this.eventUserRepository = eventUserRepository;
        this.userService = userService;
    }

    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    public Page<Event> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findAll(pageable);
    }

    @Transactional
    public Event saveEvent(RequestEvent request) {

        List<Integer> stages = request.getStages();

        List<GroupSalle> groups = groupService.findAllByStage(stages);

        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .fileName(request.getFileName())
                .stages(stages.toArray(new Integer[0]))
                .place(request.getPlace())
                .build();
        event = eventRepository.save(event);

        final Event savedEvent = event;

        List<EventGroup> eventGroups = groups.stream()
                .map(group -> EventGroup.builder()
                    .id(new EventGroupId(savedEvent.getId(), group.getId()))
                        .event(savedEvent)
                        .groupSalle(group)
                        .build())
                .collect(Collectors.toList());

        eventGroupService.saveAllEventGroup(eventGroups);

        List<UserSalle> users = userService.getUsersByStages(stages);

        assignEventToUsers(savedEvent, users);

        return savedEvent;
    }

    @Transactional
    public Event editEvent(Long eventId, RequestEvent request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + eventId));

        List<Integer> currentStages = Arrays.asList(event.getStages());

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setPlace(request.getPlace());
        event.setEventDate(request.getEventDate());
        event.setFileName(request.getFileName());
        event.setStages(request.getStages().toArray(new Integer[0]));

        Event updatedEvent = eventRepository.save(event);
        syncEventGroups(event, currentStages);

        return updatedEvent;
    }

    public void syncEventGroups(Event event, List<Integer> currentStages) {
        List<Integer> updatedStages = Arrays.asList(event.getStages());

        Set<Integer> currentSet = new HashSet<>(currentStages);
        Set<Integer> updatedSet = new HashSet<>(updatedStages);

        List<Integer> toRemove = currentStages.stream()
            .filter(stage -> !updatedSet.contains(stage))
            .toList();

        List<Integer> toAdd = updatedStages.stream()
            .filter(stage -> !currentSet.contains(stage))
            .toList();

        List<GroupSalle> groupsToRemove = groupService.findAllByStage(toRemove);
        eventGroupService.deleteEventGroupsByEventAndGroups(event.getId(), groupsToRemove);

        List<GroupSalle> groupsToAdd = groupService.findAllByStage(toAdd);
        List<EventGroup> newEventGroups = groupsToAdd.stream()
            .map(group -> EventGroup.builder()
                .id(new EventGroupId(event.getId(), group.getId()))
                .event(event)
                .groupSalle(group)
                .build())
            .toList();

        eventGroupService.saveAllEventGroup(newEventGroups);
    }


    public void assignEventToUsers(Event event, List<UserSalle> users){
        List<EventUser> eventUsers = users.stream()
            .map(user -> EventUser.builder()
                .id(new EventUserId(event.getId(), user.getId()))
                .event(event)
                .user(user)
                .status(0)
                .build()
            ).collect(Collectors.toList());

        eventUserRepository.saveAll(eventUsers);
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    public List<EventUser> getUsersByEventAndGroup(Integer eventId, Integer groupId){
        return eventUserRepository.findByEventIdAndGroupId(eventId, groupId);
    }

    @Transactional
    public void updateParticipantsAttendance(Long eventId, List<AttendanceUpdateDto> updates) throws SalleException {
        for (AttendanceUpdateDto dto : updates) {
            dto.validate();

            EventUserId id = new EventUserId(eventId, dto.getUserId());
            Optional<EventUser> optionalEventUser = eventUserRepository.findById(id);

            if (optionalEventUser.isPresent()) {
                EventUser eventUser = optionalEventUser.get();
                eventUser.setStatus(dto.getAttends());
                eventUserRepository.save(eventUser);
            }
        }
    }


}