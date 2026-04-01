package com.sallejoven.backend.controller;

import com.sallejoven.backend.mapper.ParticipantMapper;
import com.sallejoven.backend.mapper.WeeklySessionMapper;
import com.sallejoven.backend.model.dto.InitiateWeeklySessionResp;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.dto.ParticipantDto;
import com.sallejoven.backend.model.dto.PresignedPutDTO;
import com.sallejoven.backend.model.dto.WeeklySessionDto;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateRequest;
import com.sallejoven.backend.model.requestDto.FinalizeUploadsReq;
import com.sallejoven.backend.model.requestDto.WeeklySessionRequest;
import com.sallejoven.backend.service.S3V2Service;
import com.sallejoven.backend.service.WeeklySessionService;
import com.sallejoven.backend.service.WeeklySessionUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkClientException;
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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@PreAuthorize("isAuthenticated()")
@RequestMapping(value = "/api/weekly-sessions")
@RestController
@RequiredArgsConstructor
public class WeeklySessionController {

    private final WeeklySessionService weeklySessionService;
    private final WeeklySessionUserService weeklySessionUserService;
    private final WeeklySessionMapper weeklySessionMapper;
    private final ParticipantMapper participantMapper;

    @GetMapping("/paged")
    public ResponseEntity<Page<WeeklySessionDto>> getAllWeeklySessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") Boolean isPast,
            @RequestParam(required = false) Long groupId)  {
        Page<WeeklySession> sessionPage = weeklySessionService.findAll(page, size, isPast, groupId);
        Page<WeeklySessionDto> sessionDtoPage = sessionPage.map(weeklySessionMapper::toDto);
        return ResponseEntity.ok(sessionDtoPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklySessionDto> getWeeklySessionById(@PathVariable Long id) {
        return weeklySessionService.findById(id)
                .map(weeklySessionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@authz.canCreateWeeklySession(#request)")
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WeeklySessionDto> createWeeklySession(@Valid @RequestBody WeeklySessionRequest request)
             {
        WeeklySession session = weeklySessionService.saveWeeklySession(request);
        return ResponseEntity.ok(weeklySessionMapper.toDto(session));
    }

    @PreAuthorize("@authz.canManageWeeklySessionForEditOrDelete(#sessionId)")
    @PutMapping(value = "/{sessionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WeeklySessionDto> editWeeklySession(
            @PathVariable Long sessionId,
            @Valid @RequestBody WeeklySessionRequest request
    )  {
        if (request.getId() == null) {
            request.setId(sessionId);
        }
        WeeklySession session = weeklySessionService.editWeeklySession(sessionId, request);
        return ResponseEntity.ok(weeklySessionMapper.toDto(session));
    }

    @PreAuthorize("@authz.canManageWeeklySessionForEditOrDelete(#sessionId)")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteWeeklySession(@PathVariable Long sessionId) {
        if (weeklySessionService.findById(sessionId).isPresent()) {
            weeklySessionService.deleteWeeklySession(sessionId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("@authz.canManageWeeklySessionGroupParticipants(#sessionId, #groupId)")
    @GetMapping("/participants")
    public ResponseEntity<List<ParticipantDto>> getParticipantsByGroupAndSession(
            @RequestParam Long sessionId,
            @RequestParam Long groupId) {
        List<WeeklySessionUser> sessionUsers = weeklySessionUserService.findBySessionIdAndGroupId(sessionId, groupId);
        return ResponseEntity.ok(sessionUsers.stream()
                .map(participantMapper::fromWeeklySessionUser)
                .collect(Collectors.toList()));
    }

    @PreAuthorize("@authz.canManageWeeklySessionGroupParticipants(#sessionId, #groupId)")
    @PostMapping("/{sessionId}/groups/{groupId}/participants")
    public ResponseEntity<Void> updateAttendance(@PathVariable Long sessionId,
                                                @PathVariable Long groupId,
                                                @Valid @RequestBody AttendanceUpdateRequest request)  {
        weeklySessionUserService.updateParticipantsAttendance(sessionId, request.getParticipants(), groupId);
        return ResponseEntity.ok().build();
    }
}
