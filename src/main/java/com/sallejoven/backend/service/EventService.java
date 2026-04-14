package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.EventRequest;
import com.sallejoven.backend.repository.EventRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventGroupService eventGroupService;
    private final EventUserService eventUserService;
    private final AuthService authService;
    private final UserGroupService userGroupService;
    private final GroupService groupService;
    private final CenterService centerService;
    private final S3V2Service s3v2Service;

    @Value("${salle.aws.prefix:}")
    private String s3Prefix;

    public Optional<Event> findById(UUID uuid) {
        return eventRepository.findById(uuid);
    }

    public Optional<Event> findByReference(String reference) {
        return ReferenceParser.asUuid(reference).flatMap(eventRepository::findByUuid);
    }

    private static final int MAX_PAGE_SIZE = 50;

    public Page<Event> findAll(int page, int size, boolean isPast, Boolean isGeneral, LocalDate startDate, LocalDate endDate) {
        UserSalle user = authService.getCurrentUser();
        LocalDate today = ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toLocalDate();
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, effectiveSize);
        LocalDate resolvedStartDate = startDate;
        LocalDate resolvedEndDate = endDate;

        if (resolvedStartDate == null && resolvedEndDate != null) {
            resolvedStartDate = resolvedEndDate;
        } else if (resolvedEndDate == null && resolvedStartDate != null) {
            resolvedEndDate = resolvedStartDate;
        }

        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            if (resolvedStartDate != null) {
                return eventRepository.findAdminByDateRange(isGeneral, resolvedStartDate, resolvedEndDate, pageable);
            }
            return eventRepository.findAdminFilteredEvents(isGeneral, isPast, today, pageable);
        }

        List<GroupSalle> effectiveGroups = groupService.findEffectiveGroupsForUser(user);
        if (effectiveGroups.isEmpty()) {
            return Page.empty(pageable);
        }

        UUID[] effectiveGroupUuids = effectiveGroups.stream()
                .map(GroupSalle::getUuid)
                .distinct()
                .toArray(UUID[]::new);

        Integer[] userStages = effectiveGroups.stream()
                .map(GroupSalle::getStage)
                .distinct()
                .toArray(Integer[]::new);

        if (resolvedStartDate != null) {
            return eventRepository.findByDateRangeAndGroups(
                    effectiveGroupUuids,
                    userStages,
                    isGeneral,
                    resolvedStartDate,
                    resolvedEndDate,
                    pageable
            );
        }

        if (Boolean.TRUE.equals(isGeneral)) {
            return eventRepository.findGeneralEvents(userStages, isPast, today, pageable);
        } else if (Boolean.FALSE.equals(isGeneral)) {
            return eventRepository.findEventsByGroupsAndPastStatus(effectiveGroups, isPast, today, pageable);
        }

        return eventRepository.findGeneralOrUserLocalEvents(effectiveGroupUuids, userStages, isPast, today, pageable);
    }

    @Transactional
    public Event saveEvent(EventRequest requestEvent) throws IOException {
        List<Integer> stages = requestEvent.getStages();
        List<GroupSalle> groups = getTargetGroupsForEvents(requestEvent, stages);

        Event event = Event.builder()
                .name(requestEvent.getName())
                .description(requestEvent.getDescription())
                .eventDate(requestEvent.getEventDate())
                .endDate(requestEvent.getEndDate())
                .stages(stages != null ? stages.toArray(new Integer[0]) : null)
                .place(requestEvent.getPlace())
                .isGeneral(requestEvent.getIsGeneral())
                .isBlocked(false)
                .build();
        event = eventRepository.save(event);

        saveEventGroups(event, groups);
        List<UUID> groupUuids = groups.stream().map(GroupSalle::getUuid).toList();
        List<UserGroup> targetUserGroups = userGroupService.findByGroupIds(groupUuids);
        eventUserService.assignEventToUserGroups(event, targetUserGroups);
        return event;
    }

    private List<GroupSalle> getTargetGroupsForEvents(EventRequest requestEvent, List<Integer> stages) {
        if (Boolean.TRUE.equals(requestEvent.getIsGeneral())) {
            return groupService.findAllByStages(stages);
        }
        UUID centerUuid = resolveCenterUuid(requestEvent);
        if (centerUuid == null) {
            throw new SalleException(ErrorCodes.CENTER_NOT_FOUND);
        }
        return groupService.findAllByStagesAndCenter(stages, centerUuid);
    }

    @Transactional
    public Event editEvent(UUID eventUuid, EventRequest requestEvent) throws IOException {
        Event existingEvent = findById(eventUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.EVENT_NOT_FOUND));

        List<Integer> currentStages = existingEvent.getStages() == null
                ? List.of()
                : List.of(existingEvent.getStages());

        existingEvent.setName(requestEvent.getName());
        existingEvent.setDescription(requestEvent.getDescription());
        existingEvent.setEventDate(requestEvent.getEventDate());
        existingEvent.setEndDate(requestEvent.getEndDate());
        existingEvent.setStages(requestEvent.getStages().toArray(new Integer[0]));
        existingEvent.setPlace(requestEvent.getPlace());

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

        List<GroupSalle> groupsToRemove = groupService.findAllByStages(toRemoveStages);
        eventGroupService.deleteEventGroupsByEventAndGroups(event.getUuid(), groupsToRemove);

        List<GroupSalle> groupsToAdd = groupService.findAllByStages(toAddStages);
        List<EventGroup> newEventGroups = groupsToAdd.stream()
                .map(group -> EventGroup.builder().event(event).groupSalle(group).build())
                .toList();
        eventGroupService.saveAllEventGroup(newEventGroups);

        List<UUID> toAddGroupUuids = groupsToAdd.stream().map(GroupSalle::getUuid).toList();
        if (!toAddGroupUuids.isEmpty()) {
            List<UserGroup> userGroupsToAdd = userGroupService.findByGroupIds(toAddGroupUuids);
            eventUserService.assignEventToUserGroups(event, userGroupsToAdd);
        }

        List<UUID> toRemoveGroupUuids = groupsToRemove.stream().map(GroupSalle::getUuid).toList();
        if (!toRemoveGroupUuids.isEmpty()) {
            List<UserGroup> userGroupsToRemove = userGroupService.findByGroupIds(toRemoveGroupUuids);
            List<UUID> userGroupUuids = userGroupsToRemove.stream().map(UserGroup::getUuid).toList();
            eventUserService.softDeleteByEventIdAndUserGroupIds(event.getUuid(), userGroupUuids);
        }
    }

    @Transactional
    public void deleteEvent(UUID eventUuid) {
        eventUserService.softDeleteByEventId(eventUuid);
        eventGroupService.softDeleteByEventId(eventUuid);
        eventRepository.softDeleteEvent(eventUuid);
    }

    private void saveEventGroups(Event event, List<GroupSalle> groups) {
        List<EventGroup> eventGroups = groups.stream()
                .map(group -> EventGroup.builder().event(event).groupSalle(group).build())
                .toList();
        eventGroupService.saveAllEventGroup(eventGroups);
    }

    @Transactional
    public Event setBlockedStatus(UUID eventUuid, boolean blocked) {
        Event event = findById(eventUuid).orElseThrow(() -> new SalleException(ErrorCodes.EVENT_NOT_FOUND));
        event.setIsBlocked(blocked);
        return eventRepository.save(event);
    }

    @Transactional
    public Event finalizeUploads(UUID eventUuid, @Nullable String imageKey, @Nullable String pdfKey) {
        Event event = eventRepository.findById(eventUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.EVENT_NOT_FOUND));

        if (imageKey != null && !imageKey.isBlank()) {
            event.setFileName(s3v2Service.publicUrl(imageKey));
        }
        if (pdfKey != null && !pdfKey.isBlank()) {
            event.setPdf(s3v2Service.publicUrl(pdfKey));
        }
        return eventRepository.save(event);
    }

    private UUID resolveCenterUuid(EventRequest requestEvent) {
        if (requestEvent.getCenterUuid() != null && !requestEvent.getCenterUuid().isBlank()) {
            return centerService.findByReference(requestEvent.getCenterUuid()).getUuid();
        }
        return null;
    }
}
