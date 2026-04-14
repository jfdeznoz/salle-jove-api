package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.mapper.ParticipantMapper;
import com.sallejoven.backend.model.dto.ParticipantDto;
import com.sallejoven.backend.model.dto.WeeklySessionDto;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateRequest;
import com.sallejoven.backend.model.requestDto.WeeklySessionEditRequest;
import com.sallejoven.backend.model.requestDto.WeeklySessionRequest;
import com.sallejoven.backend.service.WeeklySessionService;
import com.sallejoven.backend.service.WeeklySessionUserService;
import com.sallejoven.backend.utils.ReferenceParser;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@PreAuthorize("isAuthenticated()")
@RequestMapping(value = "/api/weekly-sessions")
@RestController
@RequiredArgsConstructor
public class WeeklySessionController {

    private final WeeklySessionService weeklySessionService;
    private final WeeklySessionUserService weeklySessionUserService;
    private final ParticipantMapper participantMapper;

    @GetMapping("/paged")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Page<WeeklySessionDto>> getAllWeeklySessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") Boolean isPast,
            @RequestParam(required = false) String groupUuid,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate sessionDate) {
        UUID resolvedGroupUuid = groupUuid == null || groupUuid.isBlank()
                ? null
                : ReferenceParser.asUuid(groupUuid).orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
        Page<WeeklySession> sessionPage = weeklySessionService.findAll(page, size, isPast, resolvedGroupUuid, sessionDate);
        Page<WeeklySessionDto> sessionDtoPage = sessionPage.map(weeklySessionService::toDto);
        return ResponseEntity.ok(sessionDtoPage);
    }

    @GetMapping("/{uuid}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<WeeklySessionDto> getWeeklySessionById(@PathVariable UUID uuid) {
        return weeklySessionService.findById(uuid)
                .map(weeklySessionService::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@authz.canCreateWeeklySession(#request)")
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<WeeklySessionDto> createWeeklySession(@Valid @RequestBody WeeklySessionRequest request) {
        WeeklySession session = weeklySessionService.saveWeeklySession(request);
        return ResponseEntity.ok(weeklySessionService.toDto(session));
    }

    @PreAuthorize("@authz.canManageWeeklySessionForEditOrDelete(#sessionUuid)")
    @PutMapping(value = "/{sessionUuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<WeeklySessionDto> editWeeklySession(
            @PathVariable UUID sessionUuid,
            @Valid @RequestBody WeeklySessionEditRequest request) {
        WeeklySession session = weeklySessionService.editWeeklySession(sessionUuid, request);
        return ResponseEntity.ok(weeklySessionService.toDto(session));
    }

    @PreAuthorize("@authz.canManageWeeklySessionForEditOrDelete(#sessionUuid)")
    @DeleteMapping("/{sessionUuid}")
    public ResponseEntity<Void> deleteWeeklySession(@PathVariable UUID sessionUuid) {
        if (weeklySessionService.findByIdForEditOrDelete(sessionUuid).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        weeklySessionService.deleteWeeklySession(sessionUuid);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canViewWeeklySessionGroupParticipants(#sessionUuid, #groupUuid)")
    @GetMapping("/participants")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<ParticipantDto>> getParticipantsByGroupAndSession(
            @RequestParam UUID sessionUuid,
            @RequestParam UUID groupUuid) {
        weeklySessionService.findById(sessionUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.WEEKLY_SESSION_NOT_FOUND));
        List<WeeklySessionUser> sessionUsers = weeklySessionUserService.findBySessionIdAndGroupId(sessionUuid, groupUuid);
        return ResponseEntity.ok(sessionUsers.stream()
                .map(participantMapper::fromWeeklySessionUser)
                .collect(Collectors.toList()));
    }

    @PreAuthorize("@authz.canManageWeeklySessionGroupParticipants(#sessionUuid, #groupUuid)")
    @PostMapping("/{sessionUuid}/groups/{groupUuid}/participants")
    public ResponseEntity<Void> updateAttendance(@PathVariable UUID sessionUuid,
                                                 @PathVariable UUID groupUuid,
                                                 @Valid @RequestBody AttendanceUpdateRequest request) {
        weeklySessionService.findById(sessionUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.WEEKLY_SESSION_NOT_FOUND));
        weeklySessionUserService.updateParticipantsAttendance(sessionUuid, request.getParticipants(), groupUuid);
        return ResponseEntity.ok().build();
    }
}
