package com.sallejoven.backend.controller;

import com.sallejoven.backend.mapper.ParticipantMapper;
import com.sallejoven.backend.model.dto.EventDto;
import com.sallejoven.backend.model.dto.InitiateEventResp;
import com.sallejoven.backend.model.dto.ParticipantDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateRequest;
import com.sallejoven.backend.model.requestDto.EventRequest;
import com.sallejoven.backend.model.requestDto.FinalizeUploadsReq;
import com.sallejoven.backend.model.requestDto.ToggleBlockedRequest;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.EventService;
import com.sallejoven.backend.service.EventUserService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.S3V2Service;
import com.sallejoven.backend.service.assembler.EventAssembler;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/events")
@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventUserService eventUserService;
    private final CenterService centerService;
    private final GroupService groupService;
    private final EventAssembler eventAssembler;
    private final ParticipantMapper participantMapper;
    private final S3V2Service s3v2Service;

    @GetMapping("/paged")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Page<EventDto>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") Boolean isPast,
            @RequestParam(required = false) Boolean isGeneral,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid date range: startDate must be before or equal to endDate"
            );
        }
        return ResponseEntity.ok(eventService.findAll(page, size, isPast, isGeneral, startDate, endDate).map(eventAssembler::toDto));
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<EventDto> getEventById(@PathVariable UUID id) {
        return eventService.findById(id).map(eventAssembler::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@authz.canCreateEvent(#requestEvent)")
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InitiateEventResp> createEvent(@Valid @RequestBody EventRequest requestEvent) throws IOException {
        Event event = eventService.saveEvent(requestEvent);
        boolean wantImg = requestEvent.getImageUpload() != null && !requestEvent.getImageUpload().isBlank();
        boolean wantPdf = Boolean.TRUE.equals(requestEvent.getWantPdfUpload());
        boolean isGeneral = Boolean.TRUE.equals(requestEvent.getIsGeneral());
        Center center = isGeneral ? null : resolveCenter(requestEvent);

        var presigneds = s3v2Service.buildPresignedForEventUploads(
                isGeneral,
                center != null ? center.getUuid() : null,
                event.getEventDate(),
                event.getUuid(),
                wantImg,
                wantPdf,
                requestEvent.getImageUpload()
        );

        return ResponseEntity.ok(new InitiateEventResp(eventAssembler.toDto(event), presigneds.image(), presigneds.pdf()));
    }

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @PutMapping(value = "/{eventId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InitiateEventResp> editEventInitiate(@PathVariable UUID eventId, @Valid @RequestBody EventRequest requestEvent)
            throws IOException {
        Event event = eventService.editEvent(eventId, requestEvent);
        String imageName = requestEvent.getImageUpload() == null ? null : requestEvent.getImageUpload().trim();
        boolean wantImg = imageName != null && !imageName.isBlank();
        boolean wantPdf = Boolean.TRUE.equals(requestEvent.getWantPdfUpload());
        boolean isGeneral = Boolean.TRUE.equals(event.getIsGeneral());
        Center center = isGeneral ? null : resolveCenter(requestEvent);

        var presigneds = s3v2Service.buildPresignedForEventUploads(
                isGeneral,
                center != null ? center.getUuid() : null,
                event.getEventDate(),
                event.getUuid(),
                wantImg,
                wantPdf,
                imageName
        );

        return ResponseEntity.ok(new InitiateEventResp(eventAssembler.toDto(event), presigneds.image(), presigneds.pdf()));
    }

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @PostMapping(value = "/{eventId}/finalize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventDto> finalizeUploads(@PathVariable UUID eventId, @Valid @RequestBody FinalizeUploadsReq req) {
        return ResponseEntity.ok(eventAssembler.toDto(eventService.finalizeUploads(eventId, req.imageKey(), req.pdfKey())));
    }

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canManageEventGroupParticipants(#eventId, #groupId)")
    @GetMapping("/participants")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<ParticipantDto>> getParticipantsByGroupAndEvent(@RequestParam UUID eventId, @RequestParam UUID groupId) {
        List<EventUser> eventUsers = eventUserService.findByEventIdAndGroupId(eventId, groupId);
        return ResponseEntity.ok(eventUsers.stream().map(participantMapper::fromEventUser).collect(Collectors.toList()));
    }

    @PreAuthorize("@authz.canManageEventGroupParticipants(#eventId, #groupId)")
    @PostMapping("/{eventId}/groups/{groupId}/participants")
    public ResponseEntity<Void> updateAttendance(
            @PathVariable UUID eventId,
            @PathVariable UUID groupId,
            @Valid @RequestBody AttendanceUpdateRequest request
    ) {
        eventUserService.updateParticipantsAttendance(eventId, request.getParticipants(), groupId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @PutMapping("/{eventId}/block")
    public ResponseEntity<EventDto> toggleEventBlocked(@PathVariable UUID eventId, @Valid @RequestBody ToggleBlockedRequest request) {
        return ResponseEntity.ok(eventAssembler.toDto(eventService.setBlockedStatus(eventId, request.blocked())));
    }

    private Center resolveCenter(EventRequest requestEvent) {
        if (requestEvent.getCenterUuid() == null || requestEvent.getCenterUuid().isBlank()) {
            throw new com.sallejoven.backend.errors.SalleException(ErrorCodes.CENTER_NOT_FOUND);
        }
        return centerService.findByReference(requestEvent.getCenterUuid());
    }
}
