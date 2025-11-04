package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.EventUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventUserService {

    private final EventUserRepository eventUserRepository;
    private final EventGroupService eventGroupService;

    public void saveAll(List<EventUser> users) {
        eventUserRepository.saveAll(users);
    }

    public void save(EventUser user) {
        eventUserRepository.save(user);
    }


    public Optional<EventUser> findById(Long id) {
        return eventUserRepository.findById(id);
    }

    public List<EventUser> findConfirmedByEventIdOrdered(Long eventId) {
        var ids = eventUserRepository.findConfirmedIds(eventId);
        if (ids.isEmpty()) return List.of();
        return eventUserRepository.findByIdInFetchOrdered(ids);
    }

    public List<EventUser> findByEventIdAndGroupId(Long eventId, Long groupId) {
        return eventUserRepository.findByEventIdAndGroupId(eventId, groupId);
    }

    public List<EventUser> findByEventIdOrdered(Long eventId) {
        return eventUserRepository.findByEventIdOrdered(eventId);
    }

    @Transactional
    public void softDeleteByEventId(Long eventId) {
        eventUserRepository.softDeleteByEventId(eventId);
    }

    // EventUserService
    @Transactional
    public int softDeleteByEventIdAndUserGroupIds(Long eventId, Collection<Long> userGroupIds) {
        if (userGroupIds == null || userGroupIds.isEmpty()) return 0;
        return eventUserRepository.softDeleteByEventIdAndUserGroupIds(eventId, userGroupIds);
    }

    /** Asigna un único UserGroup a un evento (ignora si ya existe por UNIQUE(event,user_group_id)). */
    @Transactional
    public EventUser assignEventToUserGroup(Event event, UserGroup userGroup) {
        EventUser eu = EventUser.builder()
                .event(event)
                .userGroup(userGroup)
                .status(0)
                .build();
        return eventUserRepository.save(eu);
    }

    @Transactional
    public void assignEventToUserGroups(Event event, Collection<UserGroup> userGroups) {
        if (userGroups == null || userGroups.isEmpty()) return;

        List<Long> already = eventUserRepository.findUserGroupIdsByEvent(event.getId());

        List<EventUser> toInsert = userGroups.stream()
                .filter(ug -> ug != null && ug.getId() != null && !already.contains(ug.getId()))
                .map(ug -> assignEventToUserGroup(event, ug))
                .toList();

        if (!toInsert.isEmpty()) {
            saveAll(toInsert);
        }
    }

    public void assignFutureGroupEventsToUser(UserSalle user, GroupSalle group) {
        UserGroup membership = user.getGroups().stream()
                .filter(m -> m.getGroup().getId().equals(group.getId()))
                .findFirst()
                .orElse(null);

        if (membership == null) {
            return;
        }

        List<EventGroup> egList = eventGroupService.getEventGroupsByGroupId(group.getId());

        LocalDate today = LocalDate.now();
        List<Event> upcoming = egList.stream()
                .map(EventGroup::getEvent)
                .filter(evt -> {
                    LocalDate end = evt.getEndDate() != null ? evt.getEndDate() : evt.getEventDate();
                    return end != null && !end.isBefore(today);
                })
                .toList();

        for (Event e : upcoming) {
            assignEventToUserGroup(e, membership);
        }
    }

    @Transactional
    public int updateAttendanceForUserInGroup(Long eventId, Long userId, Long groupId, int status) {
        return eventUserRepository.updateStatusByEventUserAndGroup(eventId, userId, groupId, status);
    }

    @Transactional
    public void updateParticipantsAttendance(Long eventId, List<AttendanceUpdateDto> updates, Long groupId)
            throws SalleException {

        if (groupId == null) throw new SalleException(ErrorCodes.GROUP_NOT_FOUND);

        for (AttendanceUpdateDto dto : updates) {
            dto.validate();
            Long userId = dto.getUserId();
            if (userId == null) throw new SalleException(ErrorCodes.USER_NOT_FOUND);

            int updated = updateAttendanceForUserInGroup(
                    eventId, userId, groupId, dto.getAttends());

            if (updated == 0) {
                throw new SalleException(ErrorCodes.EVENT_USER_NOT_FOUND);
            }
        }
    }

    public void softDeleteByUserGroupIds(List<Long> userGroupIds, LocalDateTime when) {
        eventUserRepository.softDeleteByUserGroupIdIn(userGroupIds, when);
    }

    public void softDeleteByUserGroupIds(List<Long> userGroupIds) {
        LocalDateTime now = LocalDateTime.now();
        eventUserRepository.softDeleteByUserGroupIdIn(userGroupIds, now);
    }
}