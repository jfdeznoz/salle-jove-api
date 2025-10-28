package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.EventDto;
import com.sallejoven.backend.model.dto.ParticipantDto;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateRequest;
import com.sallejoven.backend.model.requestDto.RequestEvent;
import com.sallejoven.backend.service.EventService;
import com.sallejoven.backend.service.EventUserService;
import com.sallejoven.backend.utils.SalleConverters;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@PreAuthorize("isAuthenticated()")
@RequestMapping(value = "/api/events")
@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventUserService eventUserService;
    private final SalleConverters salleConverters;

    @GetMapping("/paged")
    public ResponseEntity<Page<EventDto>> getAllEvents(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(defaultValue = "false") Boolean isPast,
                                                    @RequestParam(required = false) Boolean isGeneral) throws SalleException {
        Page<Event> eventPage = eventService.findAll(page, size, isPast, isGeneral);
        Page<EventDto> eventDtoPage = eventPage.map(salleConverters::eventToDto);
        return ResponseEntity.ok(eventDtoPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        Optional<Event> event = eventService.findById(id);
        return event.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@authz.canCreateEvent(#requestEvent)")
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventDto> createEvent(@ModelAttribute RequestEvent requestEvent) throws IOException, SalleException {
        Event eventCreated = eventService.saveEvent(requestEvent);
        return ResponseEntity.ok(salleConverters.eventToDto(eventCreated));
    }

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @PutMapping(value = "/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventDto> editEvent(@PathVariable Long eventId,
                                              @ModelAttribute RequestEvent requestEvent)
            throws IOException, SalleException {

        if (requestEvent.getId() == null) {
            requestEvent.setId(eventId);
        }

        var updated = eventService.editEvent(requestEvent);
        return ResponseEntity.ok(salleConverters.eventToDto(updated));
    }

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        if (eventService.findById(eventId).isPresent()) {
            eventService.deleteEvent(eventId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("@authz.canManageEventGroupParticipants(#eventId, #groupId)")
    @GetMapping("/participants")
        public ResponseEntity<List<ParticipantDto>> getParticipantsByGroupAndEvent(@RequestParam Long eventId,
                                                                                   @RequestParam Long groupId) {
        List<EventUser> eventUsers = eventUserService.findByEventIdAndGroupId(eventId, groupId);
        return ResponseEntity.ok(eventUsers.stream().map(t -> {
            try {
                return salleConverters.participantDto(t);
            } catch (SalleException e) {
                throw new RuntimeException("Error al convertir EventUser a ParticipantDto", e);
            }
        }).collect(Collectors.toList()));
    }

    @PreAuthorize("@authz.canManageEventGroupParticipants(#eventId, #groupId)")
    @PostMapping("/{eventId}/groups/{groupId}/participants")
    public ResponseEntity<Void> updateAttendance(@PathVariable Long eventId,
                                                 @PathVariable Long groupId,
                                                 @RequestBody AttendanceUpdateRequest request) throws SalleException {
        eventUserService.updateParticipantsAttendance(eventId, request.getParticipants(), groupId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @PutMapping("/{eventId}/block")
    public ResponseEntity<EventDto> toggleEventBlocked(@PathVariable Long eventId,
                                                       @RequestBody Map<String, Boolean> body) throws SalleException {
        boolean blocked = body.getOrDefault("blocked", false);
        Event updated = eventService.setBlockedStatus(eventId, blocked);
        return ResponseEntity.ok(salleConverters.eventToDto(updated));
    }

}