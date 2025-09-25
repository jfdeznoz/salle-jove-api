package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventGroupService eventGroupService;
    private final EventUserService eventUserService;
    private final S3Service s3Service;
    private final AuthService authService;
    private final UserGroupService userGroupService;
    private final GroupService groupService;

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

        if (Boolean.TRUE.equals(isGeneral)) {
            return eventRepository.findGeneralEvents(isPast, today, pageable);
        }

        List<GroupSalle> groups = user.getGroups().stream()
                .map(UserGroup::getGroup)
                .distinct()
                .toList();

        if (Boolean.FALSE.equals(isGeneral)) {
            return eventRepository.findEventsByGroupsAndPastStatus(groups, isPast, today, pageable);
        }

        return eventRepository.findGeneralOrUserLocalEvents(groups, isPast, today, pageable);
    }

    @Transactional
    public Event saveEvent(RequestEvent requestEvent) throws IOException, SalleException {
        List<Integer> stages = requestEvent.getStages();
        List<UserGroup> targetUserGroups = getTargetUserGroups(requestEvent, stages);

        List<GroupSalle> groups = targetUserGroups.stream()
                .map(ug -> ug.getGroup())
                .distinct()
                .toList();

        Event event = buildEventEntity(requestEvent, stages);
        handleFileUpload(requestEvent.getFile(), event);
        event = eventRepository.save(event);

        saveEventGroups(event, groups);

        eventUserService.assignEventToUserGroups(event, targetUserGroups);

        return event;
    }

    private List<UserGroup> getTargetUserGroups(RequestEvent requestEvent, List<Integer> stages) throws SalleException {
        if (Boolean.TRUE.equals(requestEvent.getIsGeneral())) {
            // Todos los user_groups cuyos grupos estén en esos stages (en todos los centros)
            return userGroupService.findByStages(stages);
        } else {
            // Eventos locales: centro del usuario actual (por su primera membership)
            UserSalle user = authService.getCurrentUser();
            Set<UserGroup> memberships = user.getGroups(); // Set<UserGroup>
            if (memberships == null || memberships.isEmpty()) {
                throw new SalleException(ErrorCodes.USER_GROUP_NOT_FOUND);
            }
            Long centerId = memberships.iterator().next().getGroup().getCenter().getId();
            return userGroupService.findByCenterAndStages(centerId, stages);
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

    @Transactional
    public Event editEvent(RequestEvent requestEvent) throws IOException, SalleException {
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

    public void syncEventGroups(Event event, List<Integer> currentStages) throws SalleException {
        List<Integer> updatedStages = Arrays.asList(event.getStages());

        Set<Integer> currentSet = new HashSet<>(currentStages);
        Set<Integer> updatedSet = new HashSet<>(updatedStages);

        List<Integer> toRemoveStages = currentStages.stream()
                .filter(stage -> !updatedSet.contains(stage))
                .toList();

        List<Integer> toAddStages = updatedStages.stream()
                .filter(stage -> !currentSet.contains(stage))
                .toList();

        // 1) Quitar event_group de stages removidos
        List<GroupSalle> groupsToRemove = groupService.findAllByStages(toRemoveStages);
        eventGroupService.deleteEventGroupsByEventAndGroups(event.getId(), groupsToRemove);

        // 2) Añadir event_group de stages añadidos
        List<GroupSalle> groupsToAdd = groupService.findAllByStages(toAddStages);
        List<EventGroup> newEventGroups = groupsToAdd.stream()
                .map(group -> EventGroup.builder()
                        .event(event)
                        .groupSalle(group)
                        .build())
                .toList();
        eventGroupService.saveAllEventGroup(newEventGroups);

        // 3) 🔁 Sincronizar event_user

        // 3.a) Para los grupos añadidos → inscribir sus user_groups
        List<Long> toAddGroupIds = groupsToAdd.stream().map(GroupSalle::getId).toList();
        if (!toAddGroupIds.isEmpty()) {
            List<UserGroup> ugToAdd = userGroupService.findByGroupIds(toAddGroupIds);
            eventUserService.assignEventToUserGroups(event, ugToAdd);
        }

        // 3.b) Para los grupos removidos → soft delete de sus user_groups en el evento
        List<Long> toRemoveGroupIds = groupsToRemove.stream().map(GroupSalle::getId).toList();
        if (!toRemoveGroupIds.isEmpty()) {
            List<UserGroup> ugToRemove = userGroupService.findByGroupIds(toRemoveGroupIds);
            List<Long> ugIds = ugToRemove.stream().map(UserGroup::getId).toList();
            eventUserService.softDeleteByEventIdAndUserGroupIds(event.getId(), ugIds);
        }
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        eventUserService.softDeleteByEventId(eventId);
        eventGroupService.softDeleteByEventId(eventId);
        eventRepository.softDeleteEvent(eventId);
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
                        .event(event)
                        .groupSalle(group)
                        .build())
                .toList();
        eventGroupService.saveAllEventGroup(eventGroups);
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