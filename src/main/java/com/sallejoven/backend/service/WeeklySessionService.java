package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.mapper.WeeklySessionMapper;
import com.sallejoven.backend.model.dto.WeeklySessionDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.WeeklySessionEditRequest;
import com.sallejoven.backend.model.requestDto.WeeklySessionRequest;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.VitalSituationSessionRepository;
import com.sallejoven.backend.repository.WeeklySessionRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeeklySessionService {

    private final WeeklySessionRepository weeklySessionRepository;
    private final VitalSituationSessionRepository vitalSituationSessionRepository;
    private final GroupRepository groupRepository;
    private final UserGroupService userGroupService;
    private final WeeklySessionUserService weeklySessionUserService;
    private final AuthService authService;
    private final GroupService groupService;
    private final WeeklySessionMapper weeklySessionMapper;

    public Optional<WeeklySession> findById(UUID uuid) {
        return weeklySessionRepository.findById(uuid);
    }

    public Optional<WeeklySession> findByReference(String reference) {
        return ReferenceParser.asUuid(reference).flatMap(this::findById);
    }

    public Optional<WeeklySession> findByIdForEditOrDelete(UUID uuid) {
        return weeklySessionRepository.findById(uuid);
    }

    public Optional<WeeklySession> findByReferenceForEditOrDelete(String reference) {
        return ReferenceParser.asUuid(reference).flatMap(weeklySessionRepository::findById);
    }

    private static final int MAX_PAGE_SIZE = 50;

    public Page<WeeklySession> findAll(int page, int size, boolean isPast, UUID groupUuid, LocalDate sessionDate) {
        UserSalle user = authService.getCurrentUser();
        LocalDate today = ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toLocalDate();
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, effectiveSize);
        boolean onlyPublished = false;

        if (sessionDate != null) {
            if (Boolean.TRUE.equals(user.getIsAdmin())) {
                return weeklySessionRepository.findByGroupsAndSessionDate(groupRepository.findAll(), sessionDate, onlyPublished, pageable);
            }
            List<GroupSalle> effectiveGroups = groupService.findEffectiveGroupsForUser(user);
            return weeklySessionRepository.findByGroupsAndSessionDate(effectiveGroups, sessionDate, onlyPublished, pageable);
        }

        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            if (groupUuid != null) {
                return weeklySessionRepository.findByGroupUuidAndPastStatus(groupUuid, isPast, today, onlyPublished, pageable);
            }
            return weeklySessionRepository.findByGroupsAndPastStatus(groupRepository.findAll(), isPast, today, onlyPublished, pageable);
        }

        List<GroupSalle> effectiveGroups = groupService.findEffectiveGroupsForUser(user);
        if (groupUuid != null) {
            boolean hasAccess = effectiveGroups.stream().anyMatch(group -> groupUuid.equals(group.getUuid()));
            if (!hasAccess) {
                throw new SalleException(ErrorCodes.GROUP_NOT_FOUND);
            }
            return weeklySessionRepository.findByGroupUuidAndPastStatus(groupUuid, isPast, today, onlyPublished, pageable);
        }

        return weeklySessionRepository.findByGroupsAndPastStatus(effectiveGroups, isPast, today, onlyPublished, pageable);
    }

    @Transactional
    public WeeklySessionDto saveWeeklySession(WeeklySessionRequest request) {
        VitalSituationSession vitalSituationSession = resolveVitalSituationSession(request.getVitalSituationSessionUuid());
        GroupSalle group = resolveGroup(request.getGroupUuid());

        WeeklySession session = WeeklySession.builder()
                .vitalSituationSession(vitalSituationSession)
                .title(request.getTitle())
                .group(group)
                .sessionDateTime(request.getSessionDateTime())
                .observations(request.getObservations())
                .content(request.getContent())
                .status(0)
                .build();

        session = weeklySessionRepository.save(session);
        List<UserGroup> targetUserGroups = userGroupService.findByGroupId(group.getUuid());
        weeklySessionUserService.assignSessionToUserGroups(session, targetUserGroups);
        return weeklySessionMapper.toDto(session);
    }

    @Transactional
    public WeeklySessionDto editWeeklySession(UUID sessionUuid, WeeklySessionEditRequest request) {
        WeeklySession session = weeklySessionRepository.findById(sessionUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.WEEKLY_SESSION_NOT_FOUND));

        if (request.getVitalSituationSessionUuid() != null && !request.getVitalSituationSessionUuid().isBlank()) {
            session.setVitalSituationSession(resolveVitalSituationSession(request.getVitalSituationSessionUuid()));
        }
        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }
        if (request.getGroupUuid() != null && !request.getGroupUuid().isBlank()) {
            session.setGroup(resolveGroup(request.getGroupUuid()));
        }
        if (request.getSessionDateTime() != null) {
            session.setSessionDateTime(request.getSessionDateTime());
        }
        if (request.getObservations() != null) {
            session.setObservations(request.getObservations());
        }
        if (request.getContent() != null) {
            session.setContent(request.getContent());
        }

        WeeklySession saved = weeklySessionRepository.save(session);
        return weeklySessionMapper.toDto(saved);
    }

    @Transactional
    public void deleteWeeklySession(UUID sessionUuid) {
        weeklySessionRepository.softDeleteSession(sessionUuid);
        weeklySessionUserService.softDeleteBySessionId(sessionUuid);
    }

    @Transactional
    public void archiveOldSessions() {
        LocalDate today = LocalDate.now();
        List<WeeklySession> sessionsToArchive = weeklySessionRepository.findPublishedSessionsBeforeDate(today);
        for (WeeklySession session : sessionsToArchive) {
            session.setStatus(2);
            weeklySessionRepository.save(session);
        }
    }

    public WeeklySessionDto toDto(WeeklySession session) {
        return weeklySessionMapper.toDto(session);
    }

    private VitalSituationSession resolveVitalSituationSession(String uuidReference) {
        UUID uuid = ReferenceParser.asUuid(uuidReference)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
        return vitalSituationSessionRepository.findByUuid(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
    }

    private GroupSalle resolveGroup(String uuidReference) {
        UUID uuid = ReferenceParser.asUuid(uuidReference)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
        return groupRepository.findByUuid(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
    }
}
