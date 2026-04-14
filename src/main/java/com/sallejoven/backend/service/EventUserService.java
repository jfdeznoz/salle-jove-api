package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.repository.EventUserRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventUserService {

    private final EventUserRepository eventUserRepository;
    private final EventGroupService eventGroupService;
    private final UserGroupRepository userGroupRepository;
    private final AcademicStateService academicStateService;

    public void saveAll(List<EventUser> users) {
        eventUserRepository.saveAll(users);
    }

    public void save(EventUser user) {
        eventUserRepository.save(user);
    }

    public Optional<EventUser> findById(UUID uuid) {
        return eventUserRepository.findById(uuid);
    }

    public List<EventUser> findConfirmedByEventIdOrdered(UUID eventUuid) {
        var uuids = eventUserRepository.findConfirmedUuids(eventUuid);
        if (uuids.isEmpty()) {
            return List.of();
        }
        return eventUserRepository.findByUuidInFetchOrdered(uuids, academicStateService.getVisibleYear());
    }

    public List<EventUser> findByEventIdAndGroupId(UUID eventUuid, UUID groupUuid) {
        return eventUserRepository.findByEventUuidAndGroupUuid(eventUuid, groupUuid, academicStateService.getVisibleYear());
    }

    public List<EventUser> findByEventIdOrdered(UUID eventUuid) {
        return eventUserRepository.findByEventUuidOrdered(eventUuid, academicStateService.getVisibleYear());
    }

    @Transactional
    public void softDeleteByEventId(UUID eventUuid) {
        eventUserRepository.softDeleteByEventUuid(eventUuid);
    }

    @Transactional
    public int softDeleteByEventIdAndUserGroupIds(UUID eventUuid, Collection<UUID> userGroupUuids) {
        if (userGroupUuids == null || userGroupUuids.isEmpty()) {
            return 0;
        }
        List<UUID> userUuids = userGroupRepository.findDistinctUserUuidsByUuidIn(userGroupUuids);
        if (userUuids.isEmpty()) {
            return 0;
        }
        return eventUserRepository.softDeleteByEventUuidAndUserUuids(eventUuid, userUuids);
    }

    @Transactional
    public EventUser assignEventToUserGroup(Event event, UserGroup userGroup) {
        EventUser eventUser = EventUser.builder()
                .event(event)
                .user(userGroup.getUser())
                .status(0)
                .build();
        return eventUserRepository.save(eventUser);
    }

    @Transactional
    public void assignEventToUserGroups(Event event, Collection<UserGroup> userGroups) {
        if (userGroups == null || userGroups.isEmpty()) {
            return;
        }

        List<UUID> already = eventUserRepository.findUserUuidsByEvent(event.getUuid());
        List<EventUser> toInsert = userGroups.stream()
                .filter(userGroup -> userGroup != null && userGroup.getUser() != null)
                .filter(userGroup -> userGroup.getUser().getUuid() != null)
                .filter(userGroup -> !already.contains(userGroup.getUser().getUuid()))
                .map(userGroup -> EventUser.builder()
                        .event(event)
                        .user(userGroup.getUser())
                        .status(0)
                        .build())
                .toList();

        if (!toInsert.isEmpty()) {
            eventUserRepository.saveAll(toInsert);
        }
    }

    public void assignFutureGroupEventsToUser(UserSalle user, GroupSalle group) {
        boolean membershipExists = user.getGroups().stream()
                .anyMatch(membership -> membership.getDeletedAt() == null
                        && membership.getGroup() != null
                        && group.getUuid().equals(membership.getGroup().getUuid()));
        if (!membershipExists) {
            return;
        }

        List<EventGroup> eventGroups = eventGroupService.getEventGroupsByGroupId(group.getUuid());
        LocalDate today = LocalDate.now();
        List<Event> upcoming = eventGroups.stream()
                .map(EventGroup::getEvent)
                .filter(event -> {
                    LocalDate end = event.getEndDate() != null ? event.getEndDate() : event.getEventDate();
                    return end != null && !end.isBefore(today);
                })
                .toList();

        for (Event event : upcoming) {
            EventUser eventUser = EventUser.builder()
                    .event(event)
                    .user(user)
                    .status(0)
                    .build();
            eventUserRepository.save(eventUser);
        }
    }

    @Transactional
    public int updateAttendanceForUserInGroup(UUID eventUuid, UUID userUuid, UUID groupUuid, int status) {
        return eventUserRepository.updateStatusByEventUserAndGroup(
                eventUuid,
                userUuid,
                groupUuid,
                academicStateService.getVisibleYear(),
                status
        );
    }

    @Transactional
    public void updateParticipantsAttendance(UUID eventUuid, List<AttendanceUpdateDto> updates, UUID groupUuid) {
        if (groupUuid == null) {
            throw new SalleException(ErrorCodes.GROUP_NOT_FOUND);
        }

        for (AttendanceUpdateDto dto : updates) {
            dto.validate();
            UUID userUuid = resolveUserUuid(dto);
            if (userUuid == null) {
                throw new SalleException(ErrorCodes.USER_NOT_FOUND);
            }

            int updated = updateAttendanceForUserInGroup(eventUuid, userUuid, groupUuid, dto.getAttends());
            if (updated == 0) {
                throw new SalleException(ErrorCodes.EVENT_USER_NOT_FOUND);
            }
        }
    }

    public void softDeleteByUserGroupIds(List<UUID> userGroupUuids, LocalDateTime when) {
        if (userGroupUuids == null || userGroupUuids.isEmpty()) {
            return;
        }
        List<UUID> userUuids = userGroupRepository.findDistinctUserUuidsByUuidIn(userGroupUuids);
        if (userUuids.isEmpty()) {
            return;
        }
        eventUserRepository.softDeleteByUserUuidIn(userUuids, when);
    }

    public void softDeleteByUserGroupIds(List<UUID> userGroupUuids) {
        softDeleteByUserGroupIds(userGroupUuids, LocalDateTime.now());
    }

    private UUID resolveUserUuid(AttendanceUpdateDto dto) {
        if (dto.getUserUuid() != null && !dto.getUserUuid().isBlank()) {
            return ReferenceParser.asUuid(dto.getUserUuid()).orElse(null);
        }
        return null;
    }
}
