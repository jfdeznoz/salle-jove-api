package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.EventDto;
import com.sallejoven.backend.model.dto.ParticipantDto;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateRequest;
import com.sallejoven.backend.model.requestDto.RequestEvent;
import com.sallejoven.backend.service.EventService;
import com.sallejoven.backend.utils.SalleConverters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final SalleConverters salleConverters;

    @Autowired
    public EventController(EventService eventService, SalleConverters salleConverters) {
        this.eventService = eventService;
        this.salleConverters = salleConverters;
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<EventDto>> getAllEvents(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Event> eventPage = eventService.findAll(page, size);
        Page<EventDto> eventDtoPage = eventPage.map(salleConverters::eventToDto);
        return ResponseEntity.ok(eventDtoPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        Optional<Event> event = eventService.findById(id);
        return event.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    public ResponseEntity<EventDto> createEvent(@RequestBody RequestEvent event) {
        Event eventCreated = eventService.saveEvent(event);
        return ResponseEntity.ok(salleConverters.eventToDto(eventCreated));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventDto> editEvent(@PathVariable Long eventId,
                                              @RequestBody RequestEvent event) {
        Event eventCreated = eventService.editEvent(eventId, event);
        return ResponseEntity.ok(salleConverters.eventToDto(eventCreated));
    }

    /*
     * @PutMapping("/{id}")
     * public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody
     * Event eventDetails) {
     * Optional<Event> event = eventService.findById(id);
     * if (event.isPresent()) {
     * Event existingEvent = event.get();
     * existingEvent.setName(eventDetails.getName());
     * existingEvent.setEventDate(eventDetails.getEventDate());
     * existingEvent.setDivided(eventDetails.getDivided());
     * return ResponseEntity.ok(eventService.saveEvent(existingEvent));
     * }
     * return ResponseEntity.notFound().build();
     * }
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (eventService.findById(id).isPresent()) {
            eventService.deleteEvent(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/participants")
        public ResponseEntity<List<ParticipantDto>> getParticipantsByGroupAndEvent(@RequestParam Integer eventId,
                                                                    @RequestParam Integer groupId) {
        List<EventUser> eventUsers = eventService.getUsersByEventAndGroup(eventId, groupId);
        return ResponseEntity.ok(eventUsers.stream().map(salleConverters::participantDto)
                                            .collect(Collectors.toList()));
    }

    @PostMapping("/{eventId}/participants")
    public ResponseEntity<Void> updateAttendance(@PathVariable Long eventId,
                                                 @RequestBody AttendanceUpdateRequest request) throws SalleException {
        eventService.updateParticipantsAttendance(eventId, request.getParticipants());
        return ResponseEntity.ok().build();
    }

}