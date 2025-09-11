package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.EventRepository;
import com.sallejoven.backend.repository.EventUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventUserService {

    private final EventUserRepository eventUserRepository;
    private final EventRepository eventRepository;

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
        return eventUserRepository.findConfirmedByEventIdOrdered(eventId);
    }

    public List<EventUser> findByEventIdAndGroupId(Integer eventId, Integer groupId) {
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

    /* ===== Asignaciones con UserGroup (nuevo modelo) ===== */

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

    /** Asigna varios UserGroup a un evento, evitando duplicados antes de insertar. */
    @Transactional
    public void assignEventToUserGroups(Event event, Collection<UserGroup> userGroups) {
        if (userGroups == null || userGroups.isEmpty()) return;

        List<Long> already = eventUserRepository.findUserGroupIdsByEvent(event.getId());
        List<EventUser> toInsert = userGroups.stream()
                .filter(ug -> !already.contains(ug.getId()))
                .map(ug -> EventUser.builder()
                        .event(event)
                        .userGroup(ug)
                        .status(0)
                        .build())
                .toList();

        if (!toInsert.isEmpty()) {
            eventUserRepository.saveAll(toInsert);
        }
    }

    /** Asigna un UserGroup a varios eventos (por ejemplo, al mover un usuario a otro grupo y re-enganchar eventos futuros). */
    @Transactional
    public void assignEventsToUserGroup(Collection<Event> events, UserGroup userGroup) {
        if (events == null || events.isEmpty()) return;

        // Opcional: si esperas grandes volúmenes, puedes consultar existentes por lote.
        List<EventUser> toInsert = events.stream()
                .map(evt -> EventUser.builder()
                        .event(evt)
                        .userGroup(userGroup)
                        .status(0)
                        .build())
                .toList();

        // Con UNIQUE(event,user_group_id) puedes delegar de-duplicación a la BD;
        // si prefieres evitar constraint violations en logs, precalcula "already" por evento.
        eventUserRepository.saveAll(toInsert);
    }

    /** Variante por IDs primitivos si te resulta cómodo desde controladores. */
    @Transactional
    public void assignEventToUserGroups(Long eventId, Collection<Long> userGroupIds) {
        if (userGroupIds == null || userGroupIds.isEmpty()) return;
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado ID: " + eventId));

        List<Long> already = eventUserRepository.findUserGroupIdsByEvent(eventId);
        List<EventUser> toInsert = userGroupIds.stream()
                .filter(id -> !already.contains(id))
                .map(ugId -> EventUser.builder()
                        .event(event)
                        .userGroup(UserGroup.builder().id(ugId).build()) // referencia ligera
                        .status(0)
                        .build())
                .toList();

        if (!toInsert.isEmpty()) {
            eventUserRepository.saveAll(toInsert);
        }
    }

    @Transactional
    public int updateAttendanceForUserInGroup(Long eventId, Long userId, Long groupId, int status) {
        return eventUserRepository.updateStatusByEventUserAndGroup(eventId, userId, groupId, status);
    }

    @Transactional
    public void updateParticipantsAttendance(Long eventId, List<AttendanceUpdateDto> updates, Integer groupId)
            throws SalleException {

        if (groupId == null) throw new SalleException(ErrorCodes.GROUP_NOT_FOUND);
        final Long groupIdL = groupId.longValue();

        for (AttendanceUpdateDto dto : updates) {
            dto.validate();
            Long userId = dto.getUserId();
            if (userId == null) throw new SalleException(ErrorCodes.USER_NOT_FOUND);

            int updated = updateAttendanceForUserInGroup(
                    eventId, userId, groupIdL, dto.getAttends());

            if (updated == 0) {
                throw new SalleException(ErrorCodes.EVENT_USER_NOT_FOUND);
            }
        }
    }

    public void softDeleteByUserGroupIds(List<Long> userGroupIds, LocalDateTime when) {
        eventUserRepository.softDeleteByUserGroupIdIn(userGroupIds, when);
    }
}