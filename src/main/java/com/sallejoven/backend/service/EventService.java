package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.*;
import com.sallejoven.backend.model.ids.EventGroupId;
import com.sallejoven.backend.model.ids.EventUserId;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.model.requestDto.RequestEvent;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.EventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventGroupService eventGroupService;
    private final EventUserService eventUserService;
    private final GroupService groupService;
    private final UserService userService;
    private final S3Service s3Service;
    private final AuthService authService;

    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    public Page<Event> findAll(int page, int size, boolean isPast, Boolean isGeneral) throws SalleException {
        UserSalle user = authService.getCurrentUser();
        boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");
    
        LocalDate today = ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toLocalDate();
        Pageable pageable = PageRequest.of(page, size);
    
        if (isAdmin) {
            return eventRepository.findAdminFilteredEvents(isGeneral, isPast, today, pageable);
        }
    
        List<GroupSalle> groups = new ArrayList<>(user.getGroups());
    
        if (Boolean.TRUE.equals(isGeneral)) {
            return eventRepository.findGeneralEvents(isPast, today, pageable);
        }
        
        if (Boolean.FALSE.equals(isGeneral)) {
            return eventRepository.findEventsByGroupsAndPastStatus(groups, isPast, today, pageable);
        }

        return eventRepository.findGeneralOrUserLocalEvents(groups, isPast, today, pageable);
    }            

    @Transactional
    public Event saveEvent(RequestEvent requestEvent) throws IOException, SalleException {
        List<Integer> stages = requestEvent.getStages();
        List<GroupSalle> groups = getTargetGroups(requestEvent, stages);

        Event event = buildEventEntity(requestEvent, stages);
        event = eventRepository.save(event);

        handleFileUpload(requestEvent.getFile(), event);

        saveEventGroups(event, groups);

        List<UserSalle> users = userService.getUsersByStages(stages);
        eventUserService.assignEventToUsers(event, users);

        return event;
    }

    private List<GroupSalle> getTargetGroups(RequestEvent requestEvent, List<Integer> stages) throws SalleException {
        if (Boolean.TRUE.equals(requestEvent.getIsGeneral())) {
            return groupService.findAllByStages(stages);
        } else {
            UserSalle user = authService.getCurrentUser();
            Set<GroupSalle> userGroups = user.getGroups();
            if (userGroups == null || userGroups.isEmpty()) {
                throw new SalleException(ErrorCodes.USER_GROUP_NOT_FOUND);
            }
    
            GroupSalle firstGroup = userGroups.iterator().next();
            Long centerId = firstGroup.getCenter().getId();
    
            return groupService.findAllByStageAndCenter(stages, centerId);
        }
    }    

    private Event buildEventEntity(RequestEvent requestEvent, List<Integer> stages) {
        return Event.builder()
                .name(requestEvent.getName())
                .description(requestEvent.getDescription())
                .eventDate(requestEvent.getEventDate())
                .endDate(requestEvent.getEndDate())
                .stages(stages != null ? stages.toArray(new Integer[0]) : null)
                .place(requestEvent.getPlace())
                .isGeneral(requestEvent.getIsGeneral())
                .isBlocked(false)
                .build();
    }
    
    private void handleFileUpload(MultipartFile file, Event event) throws IOException {
        if (file != null && !file.isEmpty()) {
            String folderPath = "events/event_" + event.getId();
            String uploadedUrl = s3Service.uploadFile(file, folderPath);
            event.setFileName(uploadedUrl);
        }
    }
    
    private void saveEventGroups(Event event, List<GroupSalle> groups) {
        List<EventGroup> eventGroups = groups.stream()
                .map(group -> EventGroup.builder()
                        .id(new EventGroupId(event.getId(), group.getId()))
                        .event(event)
                        .groupSalle(group)
                        .build())
                .collect(Collectors.toList());
    
        eventGroupService.saveAllEventGroup(eventGroups);
    }    

    @Transactional
    public Event editEvent(RequestEvent requestEvent) throws IOException {
        Event existingEvent = eventRepository.findById(requestEvent.getId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + requestEvent.getId()));

        List<Integer> currentStages = List.of(existingEvent.getStages());

        existingEvent.setName(requestEvent.getName());
        existingEvent.setDescription(requestEvent.getDescription());
        existingEvent.setEventDate(requestEvent.getEventDate());
        existingEvent.setEndDate(requestEvent.getEndDate());
        existingEvent.setStages(requestEvent.getStages().toArray(new Integer[0]));
        existingEvent.setPlace(requestEvent.getPlace());

        MultipartFile file = requestEvent.getFile();
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

        List<GroupSalle> groupsToRemove = groupService.findAllByStages(toRemove);
        eventGroupService.deleteEventGroupsByEventAndGroups(event.getId(), groupsToRemove);

        List<GroupSalle> groupsToAdd = groupService.findAllByStages(toAdd);
        List<EventGroup> newEventGroups = groupsToAdd.stream()
                .map(group -> EventGroup.builder()
                        .id(new EventGroupId(event.getId(), group.getId()))
                        .event(event)
                        .groupSalle(group)
                        .build())
                .toList();

        eventGroupService.saveAllEventGroup(newEventGroups);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        eventUserService.softDeleteByEventId(eventId);
        eventGroupService.softDeleteByEventId(eventId);
        eventRepository.softDeleteEvent(eventId);
    }

    @Transactional
    public void updateParticipantsAttendance(Long eventId, List<AttendanceUpdateDto> updates) throws SalleException {
        for (AttendanceUpdateDto dto : updates) {
            dto.validate();
            EventUserId id = new EventUserId(eventId, dto.getUserId());
            Optional<EventUser> optionalEventUser = eventUserService.findById(id);

            if (optionalEventUser.isPresent()) {
                EventUser eventUser = optionalEventUser.get();
                eventUser.setStatus(dto.getAttends());
                eventUserService.save(eventUser);
            }
        }
    }

    @Transactional
    public Event setBlockedStatus(Long eventId, boolean blocked) throws SalleException {
        Optional<Event> optionalEvent = findById(eventId);

        if(optionalEvent.isPresent()){
            Event event = optionalEvent.get();
            
            UserSalle currentUser = authService.getCurrentUser();
            String roles = currentUser.getRoles();
    
            if (event.getIsGeneral()) {
                if (!roles.contains("ROLE_ADMIN")) {
                    throw new SalleException(ErrorCodes.BLOCK_EVENT_ERROR_ADMIN);
                }
            } else {
                if (!roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_PASTORAL_DELEGATE")) {
                    throw new SalleException(ErrorCodes.BLOCK_EVENT_ERROR);
                }
            }
    
            event.setIsBlocked(blocked);
            return eventRepository.save(event);
        }else{
            throw new SalleException(ErrorCodes.EVENT_NOT_FOUND);
        }
        
    }
}