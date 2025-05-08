package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.ids.EventUserId;
import com.sallejoven.backend.repository.EventUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventUserService {

    private final EventUserRepository eventUserRepository;

    @Autowired
    public EventUserService(EventUserRepository eventUserRepository) {
        this.eventUserRepository = eventUserRepository;
    }

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
}