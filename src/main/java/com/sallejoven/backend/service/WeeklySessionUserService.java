package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.entity.WeeklySessionBehaviorWarning;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.enums.WeeklySessionWarningType;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.WeeklySessionBehaviorWarningRepository;
import com.sallejoven.backend.repository.WeeklySessionRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private final WeeklySessionBehaviorWarningRepository weeklySessionBehaviorWarningRepository;
    private final AcademicStateService academicStateService;
    private final UserGroupRepository userGroupRepository;
    private final AuthService authService;
    private final ObservationNotificationService observationNotificationService;

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
        int year = academicStateService.getVisibleYear();
        List<WeeklySessionUser> sessionUsers = weeklySessionUserRepository.findBySessionUuidOrdered(sessionUuid)
                .stream()
                .filter(sessionUser -> sessionUser.getWeeklySession() != null
                        && sessionUser.getWeeklySession().getGroup() != null
                        && groupUuid.equals(sessionUser.getWeeklySession().getGroup().getUuid()))
                .toList();

        if (sessionUsers.isEmpty()) {
            return List.of();
        }

        Set<UUID> participantUserUuids = userGroupRepository
                .findByGroup_UuidAndYearAndDeletedAtIsNullAndUser_UuidIn(
                        groupUuid,
                        year,
                        sessionUsers.stream()
                                .map(WeeklySessionUser::getUser)
                                .filter(user -> user != null && user.getUuid() != null)
                                .map(user -> user.getUuid())
                                .toList())
                .stream()
                .filter(userGroup -> Integer.valueOf(0).equals(userGroup.getUserType()))
                .map(UserGroup::getUser)
                .filter(user -> user != null && user.getUuid() != null)
                .map(user -> user.getUuid())
                .collect(java.util.stream.Collectors.toSet());

        return sessionUsers.stream()
                .filter(sessionUser -> sessionUser.getUser() != null
                        && participantUserUuids.contains(sessionUser.getUser().getUuid()))
                .toList();
    }

    public List<WeeklySessionUser> findBySessionIdOrdered(UUID sessionUuid) {
        return weeklySessionUserRepository.findBySessionUuidOrdered(sessionUuid);
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
                .status(null)
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
                        .status(null)
                        .build())
                .toList();

        if (!toInsert.isEmpty()) {
            weeklySessionUserRepository.saveAll(toInsert);
        }
    }

    @Transactional
    public int updateAttendanceForUserInGroup(UUID sessionUuid, UUID userUuid, UUID groupUuid, Integer status) {
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
        int year = academicStateService.getVisibleYear();
        boolean currentUserAdmin = isCurrentUserAdmin();
        boolean attendanceEditable = isAttendanceEditable(session, currentUserAdmin);
        List<ObservationNotificationService.PersonalWarningNotificationItem> warningNotifications = new java.util.ArrayList<>();
        UserSalle actor = authService.getCurrentUser();

        for (AttendanceUpdateDto dto : updates) {
            dto.validate();
            UUID userUuid = resolveUserUuid(dto);
            if (userUuid == null) {
                throw new SalleException(ErrorCodes.USER_NOT_FOUND);
            }

            boolean isParticipantInGroup = userGroupRepository.findActiveByUserGroupYear(userUuid, groupUuid, year)
                    .map(userGroup -> Integer.valueOf(0).equals(userGroup.getUserType()))
                    .orElse(false);
            if (!isParticipantInGroup) {
                throw new SalleException(ErrorCodes.USER_NOT_FOUND);
            }

            WeeklySessionUser sessionUser = weeklySessionUserRepository.findBySessionUuidAndUserUuid(
                            sessionUuid,
                            userUuid)
                    .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));

            validateWeeklyAttendanceUpdate(dto, sessionUser, attendanceEditable);
            applyWeeklyAttendanceUpdate(sessionUser, dto);
            maybeUpsertWarning(sessionUser, dto, actor)
                    .ifPresent(warningNotifications::add);

            weeklySessionUserRepository.save(sessionUser);
        }

        if (!warningNotifications.isEmpty()) {
            observationNotificationService.notifyPersonalWarnings(session, warningNotifications, actor);
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

    private void validateWeeklyAttendanceUpdate(AttendanceUpdateDto dto,
                                                WeeklySessionUser sessionUser,
                                                boolean attendanceEditable) {
        if (!attendanceEditable) {
            validatePastSessionJustificationOnlyUpdate(dto, sessionUser);
        }

        if (Boolean.TRUE.equals(dto.getJustified())) {
            if (!Integer.valueOf(0).equals(dto.getAttends())) {
                throw new SalleException(ErrorCodes.STATUS_PARTICIPANT_ERROR);
            }
            if (dto.getJustificationReason() == null || dto.getJustificationReason().trim().isEmpty()) {
                throw new SalleException(ErrorCodes.STATUS_PARTICIPANT_ERROR);
            }
        }

        boolean hasWarningType = dto.getWarningType() != null;
        boolean hasWarningComment = dto.getWarningComment() != null && !dto.getWarningComment().trim().isEmpty();
        if (hasWarningType != hasWarningComment) {
            throw new SalleException(ErrorCodes.WEEKLY_SESSION_WARNING_INVALID);
        }
    }

    private void applyWeeklyAttendanceUpdate(WeeklySessionUser sessionUser, AttendanceUpdateDto dto) {
        Integer attends = dto.getAttends();

        if (attends == null) {
            sessionUser.setStatus(null);
            sessionUser.setJustified(false);
            sessionUser.setJustificationReason(null);
            return;
        }

        if (attends == 1) {
            sessionUser.setStatus(1);
            sessionUser.setJustified(false);
            sessionUser.setJustificationReason(null);
            return;
        }

        sessionUser.setStatus(0);
        sessionUser.setJustified(Boolean.TRUE.equals(dto.getJustified()));
        sessionUser.setJustificationReason(Boolean.TRUE.equals(dto.getJustified())
                ? dto.getJustificationReason().trim()
                : null);
    }

    private void validatePastSessionJustificationOnlyUpdate(AttendanceUpdateDto dto, WeeklySessionUser sessionUser) {
        if (!Integer.valueOf(0).equals(sessionUser.getStatus())) {
            throw new SalleException(ErrorCodes.SESSION_LOCKED);
        }
        if (!Integer.valueOf(0).equals(dto.getAttends())) {
            throw new SalleException(ErrorCodes.SESSION_LOCKED);
        }
        if (dto.getWarningType() != null || (dto.getWarningComment() != null && !dto.getWarningComment().isBlank())) {
            throw new SalleException(ErrorCodes.SESSION_LOCKED);
        }
    }

    private Optional<ObservationNotificationService.PersonalWarningNotificationItem> maybeUpsertWarning(
            WeeklySessionUser sessionUser,
            AttendanceUpdateDto dto,
            UserSalle actor) {
        WeeklySessionBehaviorWarning existingWarning = weeklySessionBehaviorWarningRepository
                .findByWeeklySessionUserUuidIncludingDeleted(sessionUser.getUuid())
                .orElse(null);
        WeeklySessionWarningType previousType = existingWarning != null && existingWarning.getDeletedAt() == null
                ? existingWarning.getWarningType()
                : null;
        String previousComment = existingWarning != null && existingWarning.getDeletedAt() == null
                ? existingWarning.getComment()
                : null;

        if (dto.getWarningType() == null) {
            if (existingWarning != null && existingWarning.getDeletedAt() == null) {
                existingWarning.setDeletedAt(LocalDateTime.now());
                weeklySessionBehaviorWarningRepository.save(existingWarning);
            }
            return Optional.empty();
        }

        WeeklySessionBehaviorWarning warning = existingWarning;
        if (warning == null) {
            warning = WeeklySessionBehaviorWarning.builder()
                    .weeklySessionUser(sessionUser)
                    .build();
        }
        warning.setWarningType(dto.getWarningType());
        warning.setComment(dto.getWarningComment().trim());
        warning.setCreatedByUser(actor);
        warning.setCreatedByName(buildFullName(actor));
        warning.setDeletedAt(null);
        sessionUser.setBehaviorWarning(weeklySessionBehaviorWarningRepository.save(warning));

        boolean changed = previousType != dto.getWarningType()
                || previousComment == null
                || !previousComment.trim().equals(dto.getWarningComment().trim());
        if (!changed) {
            return Optional.empty();
        }

        String participantName = sessionUser.getUser() == null
                ? "Catecúmeno"
                : (sessionUser.getUser().getName() + " " + sessionUser.getUser().getLastName()).trim();
        return Optional.of(new ObservationNotificationService.PersonalWarningNotificationItem(
                sessionUser.getUser() != null ? sessionUser.getUser().getUuid() : null,
                participantName,
                dto.getWarningType(),
                dto.getWarningComment().trim()));
    }

    private String buildFullName(UserSalle user) {
        if (user == null) {
            return "";
        }

        String firstName = user.getName() == null ? "" : user.getName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }

    private boolean isAttendanceEditable(WeeklySession session, boolean currentUserAdmin) {
        if (Integer.valueOf(2).equals(session.getStatus())) {
            throw new SalleException(ErrorCodes.SESSION_LOCKED);
        }

        if (currentUserAdmin) {
            return true;
        }

        return session.getSessionDateTime() != null
                && session.getSessionDateTime().toLocalDate().isEqual(todayMadrid());
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
