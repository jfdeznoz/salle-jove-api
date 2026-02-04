package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.WeeklySessionDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.RequestWeeklySession;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.WeeklySessionRepository;
import com.sallejoven.backend.repository.VitalSituationSessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

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
    private final AuthorityService authorityService;

    public Optional<WeeklySession> findById(Long id) {
        Optional<WeeklySession> sessionOpt = weeklySessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        WeeklySession session = sessionOpt.get();
        
        // Los catequistas solo pueden ver sesiones publicadas
        if (authorityService.isOnlyAnimator() && session.getStatus() != 1) {
            return Optional.empty(); // No puede ver sesiones en borrador o archivadas
        }
        
        return Optional.of(session);
    }

    public Page<WeeklySession> findAll(int page, int size, boolean isPast, Long groupId) throws SalleException {
        UserSalle user = authService.getCurrentUser();
        LocalDate today = ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toLocalDate();
        Pageable pageable = PageRequest.of(page, size);

        // Determinar si solo debe ver sesiones publicadas (solo catequistas)
        boolean onlyPublished = authorityService.isOnlyAnimator();

        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            // Admin puede ver todas las sesiones
            if (groupId != null) {
                return weeklySessionRepository.findByGroupIdAndPastStatus(groupId, isPast, today, onlyPublished, pageable);
            }
            // Si no hay groupId, necesitamos obtener todos los grupos
            List<GroupSalle> allGroups = groupRepository.findAll();
            return weeklySessionRepository.findByGroupsAndPastStatus(allGroups, isPast, today, onlyPublished, pageable);
        }

        // Usuario normal: solo sesiones de sus grupos
        List<GroupSalle> effectiveGroups = groupService.findEffectiveGroupsForUser(user);
        
        if (groupId != null) {
            // Verificar que el grupo pertenece a los grupos efectivos del usuario
            boolean hasAccess = effectiveGroups.stream()
                    .anyMatch(g -> g.getId().equals(groupId));
            if (!hasAccess) {
                throw new SalleException(ErrorCodes.GROUP_NOT_FOUND);
            }
            return weeklySessionRepository.findByGroupIdAndPastStatus(groupId, isPast, today, onlyPublished, pageable);
        }

        return weeklySessionRepository.findByGroupsAndPastStatus(effectiveGroups, isPast, today, onlyPublished, pageable);
    }

    @Transactional
    public WeeklySession saveWeeklySession(RequestWeeklySession request) throws SalleException {
        VitalSituationSession vss = vitalSituationSessionRepository.findById(request.getVitalSituationSessionId())
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        GroupSalle group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        WeeklySession session = WeeklySession.builder()
                .vitalSituationSession(vss)
                .title(request.getTitle())
                .group(group)
                .sessionDateTime(request.getSessionDateTime())
                .status(request.getStatus() != null ? request.getStatus() : 0) // Default DRAFT
                .build();

        session = weeklySessionRepository.save(session);

        // Asignar sesión a todos los UserGroups del grupo
        List<UserGroup> targetUserGroups = userGroupService.findByGroupId(group.getId());
        weeklySessionUserService.assignSessionToUserGroups(session, targetUserGroups);

        return session;
    }

    @Transactional
    public WeeklySession editWeeklySession(Long sessionId, RequestWeeklySession request) throws SalleException {
        WeeklySession session = weeklySessionRepository.findById(sessionId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        if (request.getVitalSituationSessionId() != null) {
            VitalSituationSession vss = vitalSituationSessionRepository.findById(request.getVitalSituationSessionId())
                    .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
            session.setVitalSituationSession(vss);
        }

        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }

        if (request.getGroupId() != null) {
            GroupSalle group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
            session.setGroup(group);
        }

        if (request.getSessionDateTime() != null) {
            session.setSessionDateTime(request.getSessionDateTime());
        }

        if (request.getStatus() != null) {
            session.setStatus(request.getStatus());
        }

        return weeklySessionRepository.save(session);
    }

    @Transactional
    public void deleteWeeklySession(Long sessionId) {
        weeklySessionRepository.softDeleteSession(sessionId);
        weeklySessionUserService.softDeleteBySessionId(sessionId);
    }


    @Transactional
    public void archiveOldSessions() {
        LocalDate today = LocalDate.now();
        List<WeeklySession> sessionsToArchive = weeklySessionRepository.findPublishedSessionsBeforeDate(today);
        for (WeeklySession session : sessionsToArchive) {
            session.setStatus(2); // ARCHIVED
            weeklySessionRepository.save(session);
        }
    }

    public WeeklySessionDto toDto(WeeklySession session) {
        GroupSalle group = session.getGroup();
        VitalSituationSession vss = session.getVitalSituationSession();

        return WeeklySessionDto.builder()
                .id(session.getId())
                .vitalSituationSessionId(vss.getId())
                .vitalSituationTitle(vss.getVitalSituation().getTitle())
                .vitalSituationSessionTitle(vss.getTitle())
                .title(session.getTitle())
                .groupId(group.getId())
                .groupName(group.getCenter().getName() + " - " + group.getCenter().getCity())
                .stage(group.getStage())
                .centerId(group.getCenter().getId())
                .centerName(group.getCenter().getName())
                .sessionDateTime(session.getSessionDateTime())
                .status(session.getStatus())
                .build();
    }
}

