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
import com.sallejoven.backend.repository.EventRepository;
import com.sallejoven.backend.repository.EventUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
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
    private final S3Service s3Service;

    @Autowired
    public EventService(EventRepository eventRepository, GroupService groupService, EventGroupService eventGroupService, EventUserRepository eventUserRepository, UserService userService, S3Service s3Service) {
        this.eventRepository = eventRepository;
        this.groupService = groupService;
        this.eventGroupService = eventGroupService;
        this.eventUserRepository = eventUserRepository;
        this.userService = userService;
        this.s3Service = s3Service;
    }

    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    public Page<Event> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findAll(pageable);
    }

    @Transactional
    public Event saveEvent(String name, String description, Date eventDate, List<Integer> stages, String place, MultipartFile file) throws IOException {

        List<GroupSalle> groups = groupService.findAllByStage(stages);

        Event event = Event.builder()
                .name(name)
                .description(description)
                .eventDate(eventDate)
                .stages(stages.toArray(new Integer[0]))
                .place(place)
                .build();

        event = eventRepository.save(event);


        if (file != null && !file.isEmpty()) {
            String folderPath = "events/event_" + event.getId();
            String uploadedUrl = s3Service.uploadFile(file, folderPath);
            event.setFileName(uploadedUrl);
        }        

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
    public Event editEvent(Long eventId, String name, String description, @DateTimeFormat(pattern = "dd/MM/yyyy") Date eventDate, List<Integer> stages, String place, MultipartFile file) throws IOException {
        Event existingEvent = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + eventId));

        List<Integer> currentStages = List.of(existingEvent.getStages());

        existingEvent.setName(name);
        existingEvent.setDescription(description);
        existingEvent.setEventDate(eventDate);
        existingEvent.setStages(stages.toArray(new Integer[0]));
        existingEvent.setPlace(place);

        if (file != null && !file.isEmpty()) {
            String folderPath = "events/event_" + existingEvent.getId();
            String uploadedUrl = s3Service.uploadFile(file, folderPath);
            existingEvent.setFileName(uploadedUrl);
        }

        Event updatedEvent = eventRepository.save(existingEvent);

        syncEventGroups(updatedEvent, currentStages);

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

    public List<EventUser> getUsersByEventOrdered(Long eventId) {
        return eventUserRepository.findByEventIdOrdered(eventId);
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