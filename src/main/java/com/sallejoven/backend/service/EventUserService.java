package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.ids.EventUserId;
import com.sallejoven.backend.repository.EventRepository;
import com.sallejoven.backend.repository.EventUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public Optional<EventUser> findById(EventUserId id) {
        return eventUserRepository.findById(id);
    }

    @Transactional
    public void softDeleteByEventId(Long eventId) {
        eventUserRepository.softDeleteByEventId(eventId);
    }

    public List<EventUser> findByEventIdAndGroupId(Integer eventId, Integer groupId) {
        return eventUserRepository.findByEventIdAndGroupId(eventId, groupId);
    }

    public List<EventUser> findByEventIdOrdered(Long eventId) {
        return eventUserRepository.findByEventIdOrdered(eventId);
    }

    @Transactional
    public void assignEventToUsers(Event event, List<UserSalle> users) {
        List<EventUser> eventUsers = users.stream()
            .map(user -> EventUser.builder()
                .id(new EventUserId(event.getId(), user.getId()))
                .event(event)
                .user(user)
                .status(0)
                .build())
            .toList();
        eventUserRepository.saveAll(eventUsers);
    }

    @Transactional
    public void assignEventToUsers(Integer eventId, List<UserSalle> users) {
        Event event = eventRepository.findById(eventId.longValue())
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado ID: " + eventId));

        List<EventUser> eventUsers = users.stream()
            .map(user -> EventUser.builder()
                .id(new EventUserId(event.getId(), user.getId()))
                .event(event)
                .user(user)
                .status(0)
                .build())
            .toList();

        eventUserRepository.saveAll(eventUsers);
    }
}