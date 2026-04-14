package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.WeeklySessionRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeeklySessionUserService {

    private final WeeklySessionUserRepository weeklySessionUserRepository;
    private final WeeklySessionRepository weeklySessionRepository;
    private final AcademicStateService academicStateService;
    private final UserGroupRepository userGroupRepository;

    public void saveAll(List<WeeklySessionUser> users) {
        weeklySessionUserRepository.saveAll(users);
    }

    public void save(WeeklySessionUser user) {
        weeklySessionUserRepository.save(user);
    }

    public Optional<WeeklySessionUser> findById(UUID uuid) {
        return weeklySessionUserRepository.findById(uuid);
    }

    public List<WeeklySessionUser> findBySessionIdAndGroupId(UUID sessionUuid, UUID groupUuid) {
        return weeklySessionUserRepository.findBySessionUuidAndGroupUuid(
                sessionUuid,
                groupUuid,
                academicStateService.getVisibleYear()
        );
    }

    public List<WeeklySessionUser> findBySessionIdOrdered(UUID sessionUuid) {
        return weeklySessionUserRepository.findBySessionUuidOrdered(sessionUuid, academicStateService.getVisibleYear());
    }

    @Transactional
    public void softDeleteBySessionId(UUID sessionUuid) {
        weeklySessionUserRepository.softDeleteBySessionUuid(sessionUuid);
    }

    @Transactional
    public int softDeleteBySessionIdAndUserGroupIds(UUID sessionUuid, Collection<UUID> userGroupUuids) {
        if (userGroupUuids == null || userGroupUuids.isEmpty()) {
            return 0;
        }
        List<UUID> userUuids = userGroupRepository.findDistinctUserUuidsByUuidIn(userGroupUuids);
        if (userUuids.isEmpty()) {
            return 0;
        }
        return weeklySessionUserRepository.softDeleteBySessionUuidAndUserUuids(sessionUuid, userUuids);
    }

    @Transactional
    public WeeklySessionUser assignSessionToUserGroup(WeeklySession session, UserGroup userGroup) {
        WeeklySessionUser sessionUser = WeeklySessionUser.builder()
                .weeklySession(session)
                .user(userGroup.getUser())
                .status(0)
                .build();
        return weeklySessionUserRepository.save(sessionUser);
    }

    @Transactional
    public void assignSessionToUserGroups(WeeklySession session, Collection<UserGroup> userGroups) {
        if (userGroups == null || userGroups.isEmpty()) {
            return;
        }

        List<UUID> already = weeklySessionUserRepository.findUserUuidsBySession(session.getUuid());
        List<WeeklySessionUser> toInsert = userGroups.stream()
                .filter(userGroup -> userGroup != null && userGroup.getUser() != null)
                .filter(userGroup -> userGroup.getUser().getUuid() != null)
                .filter(userGroup -> !already.contains(userGroup.getUser().getUuid()))
                .map(userGroup -> WeeklySessionUser.builder()
                        .weeklySession(session)
                        .user(userGroup.getUser())
                        .status(0)
                        .build())
                .toList();

        if (!toInsert.isEmpty()) {
            weeklySessionUserRepository.saveAll(toInsert);
        }
    }

    @Transactional
    public int updateAttendanceForUserInGroup(UUID sessionUuid, UUID userUuid, UUID groupUuid, int status) {
        return weeklySessionUserRepository.updateStatusBySessionUserAndGroup(
                sessionUuid,
                userUuid,
                groupUuid,
                academicStateService.getVisibleYear(),
                status
        );
    }

    @Transactional
    public void updateParticipantsAttendance(UUID sessionUuid, List<AttendanceUpdateDto> updates, UUID groupUuid) {
        if (groupUuid == null) {
            throw new SalleException(ErrorCodes.GROUP_NOT_FOUND);
        }
        WeeklySession session = weeklySessionRepository.findById(sessionUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.WEEKLY_SESSION_NOT_FOUND));

        ensureAttendanceEditable(session);

        for (AttendanceUpdateDto dto : updates) {
            dto.validate();
            UUID userUuid = resolveUserUuid(dto);
            if (userUuid == null) {
                throw new SalleException(ErrorCodes.USER_NOT_FOUND);
            }

            WeeklySessionUser sessionUser = weeklySessionUserRepository.findBySessionUserAndGroup(
                            sessionUuid,
                            userUuid,
                            groupUuid,
                            academicStateService.getVisibleYear())
                    .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));

            sessionUser.setStatus(dto.getAttends());
            if (dto.getJustified() != null) {
                sessionUser.setJustified(dto.getJustified());
            }
            if (dto.getJustificationReason() != null || Boolean.FALSE.equals(dto.getJustified())) {
                sessionUser.setJustificationReason(dto.getJustificationReason());
            }

            weeklySessionUserRepository.save(sessionUser);
        }
    }

    public void softDeleteByUserGroupIds(List<UUID> userGroupUuids, LocalDateTime when) {
        if (userGroupUuids == null || userGroupUuids.isEmpty()) {
            return;
        }
        List<UUID> userUuids = userGroupRepository.findDistinctUserUuidsByUuidIn(userGroupUuids);
        if (userUuids.isEmpty()) {
            return;
        }
        weeklySessionUserRepository.softDeleteByUserUuidIn(userUuids, when);
    }

    public void softDeleteByUserGroupIds(List<UUID> userGroupUuids) {
        softDeleteByUserGroupIds(userGroupUuids, LocalDateTime.now());
    }

    private UUID resolveUserUuid(AttendanceUpdateDto dto) {
        if (dto.getUserUuid() != null && !dto.getUserUuid().isBlank()) {
            return ReferenceParser.asUuid(dto.getUserUuid()).orElse(null);
        }
        return null;
    }

    private void ensureAttendanceEditable(WeeklySession session) {
        if (Integer.valueOf(2).equals(session.getStatus())) {
            throw new SalleException(ErrorCodes.SESSION_LOCKED);
        }

        if (isCurrentUserAdmin()) {
            return;
        }

        if (session.getSessionDateTime() == null
                || !session.getSessionDateTime().toLocalDate().isEqual(todayMadrid())) {
            throw new SalleException(ErrorCodes.SESSION_LOCKED);
        }
    }

    private boolean isCurrentUserAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private LocalDate todayMadrid() {
        return java.time.ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toLocalDate();
    }
}
