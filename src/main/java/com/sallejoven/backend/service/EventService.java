package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.EventRequest;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.EventRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final AuthService authService;
    private final UserGroupService userGroupService;
    private final GroupService groupService;
    private final S3V2Service s3v2Service;


    @Value("${salle.aws.prefix:}")
    private String s3Prefix;

    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    public Page<Event> findAll(int page, int size, boolean isPast, Boolean isGeneral) {
        UserSalle user = authService.getCurrentUser();

        LocalDate today = ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toLocalDate();
        Pageable pageable = PageRequest.of(page, size);

        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            return eventRepository.findAdminFilteredEvents(isGeneral, isPast, today, pageable);
        }

        if (Boolean.TRUE.equals(isGeneral)) {
            return eventRepository.findGeneralEvents(isPast, today, pageable);
        }

        List<GroupSalle> effectiveGroups = groupService.findEffectiveGroupsForUser(user);

        if (Boolean.FALSE.equals(isGeneral)) {
            return eventRepository.findEventsByGroupsAndPastStatus(effectiveGroups, isPast, today, pageable);
        }

        return eventRepository.findGeneralOrUserLocalEvents(effectiveGroups, isPast, today, pageable);
    }

    @Transactional
    public Event saveEvent(EventRequest requestEvent) throws IOException {
        List<Integer> stages = requestEvent.getStages();

        List<GroupSalle> groups = getTargetGroupsForEvents(requestEvent, stages);

        Event event = buildEventEntity(requestEvent, stages);
        event = eventRepository.save(event);

        saveEventGroups(event, groups);

        List<Long> groupIds = groups.stream().map(GroupSalle::getId).toList();
        List<UserGroup> targetUserGroups = userGroupService.findByGroupIds(groupIds);
        eventUserService.assignEventToUserGroups(event, targetUserGroups);

        return event;
    }

    private List<GroupSalle> getTargetGroupsForEvents(EventRequest requestEvent, List<Integer> stages) {
        if (Boolean.TRUE.equals(requestEvent.getIsGeneral())) {
            // todos los grupos de esos stages, independientemente de si tienen usuarios
            return groupService.findAllByStages(stages);
        }
        Long centerId = requestEvent.getCenterId();
        if (centerId == null) throw new SalleException(ErrorCodes.CENTER_NOT_FOUND);
        // grupos del centro + stages
        return groupService.findAllByStagesAndCenter(stages,centerId);
    }

private Event buildEventEntity(EventRequest requestEvent, List<Integer> stages) {
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
    public Event editEvent(EventRequest requestEvent) throws IOException {
        Event existingEvent = eventRepository.findById(requestEvent.getId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + requestEvent.getId()));

        List<Integer> currentStages = List.of(existingEvent.getStages());

        existingEvent.setName(requestEvent.getName());
        existingEvent.setDescription(requestEvent.getDescription());
        existingEvent.setEventDate(requestEvent.getEventDate());
        existingEvent.setEndDate(requestEvent.getEndDate());
        existingEvent.setStages(requestEvent.getStages().toArray(new Integer[0]));
        existingEvent.setPlace(requestEvent.getPlace());

        //handleUploads(requestEvent.getFile(), requestEvent.getPdf(), existingEvent);

        existingEvent = eventRepository.save(existingEvent);

        syncEventGroups(existingEvent, currentStages);

        return existingEvent;
    }


    public void syncEventGroups(Event event, List<Integer> currentStages) {
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

    private String slugify(String input) {
        if (input == null || input.isBlank()) return "evento";
        String s = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.toLowerCase()
                .replaceAll("[^a-z0-9\\s_-]", "")
                .replaceAll("[\\s]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[-_]+|[-_]+$", "");
        return s.isBlank() ? "evento" : s;
    }

    private String withPrefix(String path) {
        if (s3Prefix == null || s3Prefix.isBlank()) return normalize(path);
        String prefix = s3Prefix.endsWith("/") ? s3Prefix : s3Prefix + "/";
        return normalize(prefix + path);
    }

    private String normalize(String path) {
        String p = path.replaceAll("/{2,}", "/");
        if (p.startsWith("/")) p = p.substring(1);
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
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
    public Event setBlockedStatus(Long eventId, boolean blocked) {
        Optional<Event> optionalEvent = findById(eventId);

        if(optionalEvent.isPresent()){
            Event event = optionalEvent.get();
            event.setIsBlocked(blocked);
            return eventRepository.save(event);
        }else{
            throw new SalleException(ErrorCodes.EVENT_NOT_FOUND);
        }

    }

    @Transactional
    public Event finalizeUploads(Long eventId, @Nullable String imageKey, @Nullable String pdfKey) {
        Event ev = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado: " + eventId));

        if (imageKey != null && !imageKey.isBlank()) {
            String oldUrl = ev.getFileName();
            if (oldUrl != null && !oldUrl.isBlank()) {
                String oldKey = s3v2Service.keyFromUrl(oldUrl);
                if (oldKey != null && !oldKey.isBlank() && !oldKey.equals(imageKey)) {
                    s3v2Service.deleteObject(oldKey);
                }
            }
            ev.setFileName(s3v2Service.publicUrl(imageKey));
        }

        if (pdfKey != null && !pdfKey.isBlank()) {
            String oldUrl = ev.getPdf();
            if (oldUrl != null && !oldUrl.isBlank()) {
                String oldKey = s3v2Service.keyFromUrl(oldUrl);
                if (oldKey != null && !oldKey.isBlank() && !oldKey.equals(pdfKey)) {
                    s3v2Service.deleteObject(oldKey);
                }
            }
            ev.setPdf(s3v2Service.publicUrl(pdfKey));
        }

        return eventRepository.save(ev);
    }

}