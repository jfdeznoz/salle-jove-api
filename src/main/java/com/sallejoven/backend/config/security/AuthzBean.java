package com.sallejoven.backend.config.security;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.EventRequest;
import com.sallejoven.backend.model.requestDto.GroupRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.repository.EventGroupRepository;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserPendingRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.AuthorityService;
import com.sallejoven.backend.service.EventService;
import com.sallejoven.backend.service.VitalSituationService;
import com.sallejoven.backend.service.WeeklySessionService;
import com.sallejoven.backend.utils.ReferenceParser;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component("authz")
public class AuthzBean {

    private final UserCenterRepository userCenterRepo;
    private final UserRepository userRepo;
    private final UserGroupRepository userGroupRepo;
    private final CenterRepository centerRepo;
    private final GroupRepository groupRepository;
    private final AcademicStateService academicStateService;
    private final EventService eventService;
    private final EventGroupRepository eventGroupRepo;
    private final AuthorityService authorityService;
    private final UserPendingRepository userPendingRepo;
    private final WeeklySessionService weeklySessionService;
    private final VitalSituationService vitalSituationService;

    private Set<String> auths() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private boolean isAdmin(Set<String> authorities) {
        return authorities.contains("ROLE_ADMIN");
    }

    private UUID currentUserUuidOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return userRepo.findByEmail(authentication.getName()).map(UserSalle::getUuid).orElse(null);
    }

    private boolean isSelf(UUID userUuid) {
        UUID me = currentUserUuidOrNull();
        return me != null && me.equals(userUuid);
    }

    public boolean isAnyManagerType() {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        return !authorityService.extractCenterIdsForYear(authorities, year).isEmpty();
    }

    public boolean canViewUserStats(String userReference) {
        UUID userUuid = ReferenceParser.asUuid(userReference)
                .flatMap(userRepo::findByUuid)
                .map(UserSalle::getUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
        return canViewUserGroups(userUuid);
    }

    public boolean canViewUserGroups(UUID userUuid) {
        Set<String> authorities = auths();
        if (isAdmin(authorities) || isSelf(userUuid)) {
            return true;
        }

        int year = academicStateService.getVisibleYear();
        Set<UUID> targetCenterUuids = new HashSet<>();
        Set<UUID> targetGroupUuids = new HashSet<>();

        userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year).forEach(userGroup -> {
            if (userGroup.getGroup() != null) {
                targetGroupUuids.add(userGroup.getGroup().getUuid());
                if (userGroup.getGroup().getCenter() != null) {
                    targetCenterUuids.add(userGroup.getGroup().getCenter().getUuid());
                }
            }
        });

        userCenterRepo.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year).forEach(userCenter -> {
            if (userCenter.getCenter() != null) {
                targetCenterUuids.add(userCenter.getCenter().getUuid());
            }
        });

        for (UUID centerUuid : targetCenterUuids) {
            if (authorities.contains("CENTER:" + centerUuid + ":PASTORAL_DELEGATE:" + year)
                    || authorities.contains("CENTER:" + centerUuid + ":GROUP_LEADER:" + year)) {
                return true;
            }
        }
        for (UUID groupUuid : targetGroupUuids) {
            if (authorities.contains("GROUP:" + groupUuid + ":ANIMATOR:" + year)) {
                return true;
            }
        }
        return false;
    }

    public boolean canViewUserCenters(UUID userUuid) {
        var authorities = auths();
        if (isAdmin(authorities) || isSelf(userUuid)) {
            return true;
        }

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }

        var viewerCenters = authorityService.extractCenterIdsForYear(authorities, year);
        if (viewerCenters.isEmpty()) {
            return false;
        }

        Set<UUID> targetCenters = new HashSet<>();
        userCenterRepo.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year).forEach(userCenter -> {
            if (userCenter.getCenter() != null) {
                targetCenters.add(userCenter.getCenter().getUuid());
            }
        });
        userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year).forEach(userGroup -> {
            if (userGroup.getGroup() != null && userGroup.getGroup().getCenter() != null) {
                targetCenters.add(userGroup.getGroup().getCenter().getUuid());
            }
        });
        return targetCenters.stream().anyMatch(viewerCenters::contains);
    }

    public boolean canManageCenterAsDelegate(UUID centerUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        return year != null && authorities.contains("CENTER:" + centerUuid + ":PASTORAL_DELEGATE:" + year);
    }

    public boolean canManageUserCenterAsDelegate(UUID userCenterUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        return userCenterRepo.findById(userCenterUuid)
                .map(userCenter -> userCenter.getCenter() != null ? userCenter.getCenter().getUuid() : null)
                .map(centerUuid -> authorities.contains("CENTER:" + centerUuid + ":PASTORAL_DELEGATE:" + year))
                .orElse(false);
    }

    public boolean hasCenterRole(UUID centerUuid, String... roles) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        for (String role : roles) {
            if (authorities.contains("CENTER:" + centerUuid + ":" + role + ":" + year)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasGroupRole(UUID groupUuid, String... roles) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        for (String role : roles) {
            if (authorities.contains("GROUP:" + groupUuid + ":" + role + ":" + year)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCenterOfGroup(UUID groupUuid, String... roles) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        GroupSalle group = groupRepository.findById(groupUuid).orElse(null);
        if (group == null || group.getCenter() == null) {
            return false;
        }
        for (String role : roles) {
            if (authorities.contains("CENTER:" + group.getCenter().getUuid() + ":" + role + ":" + year)) {
                return true;
            }
        }
        return false;
    }

    public boolean canCreateEvent(EventRequest req) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        if (req == null || Boolean.TRUE.equals(req.getIsGeneral())) {
            return false;
        }
        UUID centerUuid = requestCenterId(req);
        if (centerUuid == null) {
            return false;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        return year != null && (authorities.contains("CENTER:" + centerUuid + ":PASTORAL_DELEGATE:" + year)
                || authorities.contains("CENTER:" + centerUuid + ":GROUP_LEADER:" + year));
    }

    public boolean canManageEventForEditOrDelete(UUID eventUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        var eventOpt = eventService.findById(eventUuid);
        if (eventOpt.isEmpty() || Boolean.TRUE.equals(eventOpt.get().getIsGeneral())) {
            return false;
        }
        var centerUuids = eventGroupRepo.findByEventUuid(eventUuid).stream()
                .filter(eventGroup -> eventGroup.getGroupSalle() != null && eventGroup.getGroupSalle().getCenter() != null)
                .map(eventGroup -> eventGroup.getGroupSalle().getCenter().getUuid())
                .collect(Collectors.toSet());
        return centerUuids.stream().anyMatch(centerUuid ->
                authorities.contains("CENTER:" + centerUuid + ":PASTORAL_DELEGATE:" + year)
                        || authorities.contains("CENTER:" + centerUuid + ":GROUP_LEADER:" + year));
    }

    public boolean canDownloadEventReports(UUID eventUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        Set<UUID> managedCenterUuids = authorityService.extractCenterIdsForYear(authorities, year);
        if (managedCenterUuids.isEmpty()) {
            return false;
        }
        return eventGroupRepo.findByEventUuid(eventUuid).stream()
                .filter(eventGroup -> eventGroup.getGroupSalle() != null && eventGroup.getGroupSalle().getCenter() != null)
                .map(eventGroup -> eventGroup.getGroupSalle().getCenter().getUuid())
                .anyMatch(managedCenterUuids::contains);
    }

    public boolean canManageEvent(UUID eventUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }

        var eventGroups = eventGroupRepo.findByEventUuid(eventUuid);
        var centerUuids = eventGroups.stream()
                .filter(eventGroup -> eventGroup.getGroupSalle() != null && eventGroup.getGroupSalle().getCenter() != null)
                .map(eventGroup -> eventGroup.getGroupSalle().getCenter().getUuid())
                .collect(Collectors.toSet());
        for (UUID centerUuid : centerUuids) {
            if (authorities.contains("CENTER:" + centerUuid + ":PASTORAL_DELEGATE:" + year)
                    || authorities.contains("CENTER:" + centerUuid + ":GROUP_LEADER:" + year)) {
                return true;
            }
        }

        UUID me = currentUserUuidOrNull();
        if (me == null) {
            return false;
        }
        return eventGroups.stream()
                .map(eventGroup -> eventGroup.getGroupSalle())
                .filter(group -> group != null)
                .anyMatch(group -> userGroupRepo.existsByUser_UuidAndGroup_UuidAndYearAndDeletedAtIsNullAndUserType(
                        me,
                        group.getUuid(),
                        year,
                        1
                ));
    }

    public boolean canManageEventGroupParticipants(UUID eventUuid, UUID groupUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }

        boolean groupIsInEvent = eventGroupRepo.findByEventUuid(eventUuid).stream()
                .anyMatch(eventGroup -> eventGroup.getGroupSalle() != null && groupUuid.equals(eventGroup.getGroupSalle().getUuid()));
        if (!groupIsInEvent) {
            return false;
        }

        UUID me = currentUserUuidOrNull();
        if (me != null && userGroupRepo.existsByUser_UuidAndGroup_UuidAndYearAndDeletedAtIsNullAndUserType(me, groupUuid, year, 1)) {
            return true;
        }

        GroupSalle group = groupRepository.findById(groupUuid).orElse(null);
        return group != null && group.getCenter() != null && (
                authorities.contains("CENTER:" + group.getCenter().getUuid() + ":PASTORAL_DELEGATE:" + year)
                        || authorities.contains("CENTER:" + group.getCenter().getUuid() + ":GROUP_LEADER:" + year)
        );
    }

    public boolean canCreateWeeklySession(com.sallejoven.backend.model.requestDto.WeeklySessionRequest req) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        UUID groupUuid = requestGroupId(req);
        if (groupUuid == null) {
            return false;
        }
        return hasGroupRole(groupUuid, "ANIMATOR") || hasCenterOfGroup(groupUuid, "PASTORAL_DELEGATE", "GROUP_LEADER");
    }

    public boolean canManageWeeklySessionForEditOrDelete(UUID sessionUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        var sessionOpt = weeklySessionService.findByIdForEditOrDelete(sessionUuid);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        var session = sessionOpt.get();
        var group = session.getGroup();
        if (group == null) {
            return false;
        }
        if (Integer.valueOf(0).equals(session.getStatus())
                && authorities.contains("GROUP:" + group.getUuid() + ":ANIMATOR:" + year)) {
            return true;
        }
        return group.getCenter() != null && (
                authorities.contains("CENTER:" + group.getCenter().getUuid() + ":PASTORAL_DELEGATE:" + year)
                        || authorities.contains("CENTER:" + group.getCenter().getUuid() + ":GROUP_LEADER:" + year)
        );
    }

    public boolean canViewWeeklySessionGroupParticipants(UUID sessionUuid, UUID groupUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        var sessionOpt = weeklySessionService.findById(sessionUuid);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        var session = sessionOpt.get();
        if (session.getGroup() == null || !groupUuid.equals(session.getGroup().getUuid())) {
            return false;
        }
        if (authorityService.isOnlyAnimator() && !Integer.valueOf(1).equals(session.getStatus())) {
            return false;
        }
        UUID me = currentUserUuidOrNull();
        if (me != null && userGroupRepo.existsByUser_UuidAndGroup_UuidAndYearAndDeletedAtIsNullAndUserType(me, groupUuid, year, 1)) {
            return true;
        }
        return session.getGroup().getCenter() != null && (
                authorities.contains("CENTER:" + session.getGroup().getCenter().getUuid() + ":PASTORAL_DELEGATE:" + year)
                        || authorities.contains("CENTER:" + session.getGroup().getCenter().getUuid() + ":GROUP_LEADER:" + year)
        );
    }

    public boolean canManageWeeklySessionGroupParticipants(UUID sessionUuid, UUID groupUuid) {
        if (!canViewWeeklySessionGroupParticipants(sessionUuid, groupUuid)) {
            return false;
        }

        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }

        return weeklySessionService.findById(sessionUuid)
                .map(session -> session.getSessionDateTime() != null
                        && session.getSessionDateTime().toLocalDate().isEqual(todayMadrid()))
                .orElse(false);
    }

    private LocalDate todayMadrid() {
        return java.time.ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toLocalDate();
    }

    public boolean canCreateOrEditVitalSituation(com.sallejoven.backend.model.requestDto.VitalSituationRequest req) {
        return isAnyManagerType() || isAdmin(auths());
    }

    public boolean canEditVitalSituation(UUID uuid) {
        if (isAdmin(auths())) {
            return true;
        }
        return vitalSituationService.findById(uuid)
                .filter(vitalSituation -> !Boolean.TRUE.equals(vitalSituation.isDefault()))
                .isPresent()
                && isAnyManagerType();
    }

    public boolean canDeleteVitalSituation(UUID uuid) {
        return canEditVitalSituation(uuid);
    }

    public boolean canCreateOrEditVitalSituationSession(com.sallejoven.backend.model.requestDto.VitalSituationSessionRequest req) {
        return isAnyManagerType() || isAdmin(auths());
    }

    public boolean canEditVitalSituationSession(UUID uuid) {
        if (isAdmin(auths())) {
            return true;
        }
        return vitalSituationService.findSessionById(uuid)
                .filter(session -> !Boolean.TRUE.equals(session.isDefault()))
                .isPresent()
                && isAnyManagerType();
    }

    public boolean canDeleteVitalSituationSession(UUID uuid) {
        return canEditVitalSituationSession(uuid);
    }

    private static String normalizeRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ROLE_PARTICIPANT";
        }
        String role = raw.trim().toUpperCase();
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    public boolean canModeratePending(UUID pendingUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        UserPending pending = userPendingRepo.findById(pendingUuid).orElse(null);
        if (pending == null) {
            return false;
        }

        String role = normalizeRole(pending.getRoles());
        if (pending.getCenterUuid() != null) {
            if ("ROLE_PASTORAL_DELEGATE".equals(role)) {
                return false;
            }
            return hasCenterRole(pending.getCenterUuid(), "PASTORAL_DELEGATE");
        }
        if (pending.getGroupUuid() != null) {
            GroupSalle group = groupRepository.findById(pending.getGroupUuid()).orElse(null);
            return group != null && group.getCenter() != null
                    && hasCenterRole(group.getCenter().getUuid(), "PASTORAL_DELEGATE", "GROUP_LEADER");
        }
        return false;
    }

    public boolean canCreateUser(UserSalleRequest req) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null || req == null) {
            return false;
        }
        String role = normalizeRole(req.getRol());

        UUID centerUuid = requestCenterId(req);
        if (centerUuid != null) {
            return "ROLE_GROUP_LEADER".equals(role)
                    && authorities.contains("CENTER:" + centerUuid + ":PASTORAL_DELEGATE:" + year);
        }

        UUID groupUuid = requestGroupId(req);
        if (groupUuid != null) {
            GroupSalle group = groupRepository.findById(groupUuid).orElse(null);
            return group != null && group.getCenter() != null && (
                    authorities.contains("CENTER:" + group.getCenter().getUuid() + ":PASTORAL_DELEGATE:" + year)
                            || authorities.contains("CENTER:" + group.getCenter().getUuid() + ":GROUP_LEADER:" + year)
            );
        }
        return false;
    }

    public boolean canManageUser(UUID userUuid) {
        var authorities = auths();
        if (isAdmin(authorities)) {
            return true;
        }
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }

        Set<UUID> targetCenters = new HashSet<>();
        userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year).forEach(userGroup -> {
            if (userGroup.getGroup() != null && userGroup.getGroup().getCenter() != null) {
                targetCenters.add(userGroup.getGroup().getCenter().getUuid());
            }
        });
        userCenterRepo.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year).forEach(userCenter -> {
            if (userCenter.getCenter() != null) {
                targetCenters.add(userCenter.getCenter().getUuid());
            }
        });

        return targetCenters.stream().anyMatch(centerUuid ->
                authorities.contains("CENTER:" + centerUuid + ":PASTORAL_DELEGATE:" + year)
                        || authorities.contains("CENTER:" + centerUuid + ":GROUP_LEADER:" + year));
    }

    public boolean canEditUserAsAnimator(UUID userUuid) {
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) {
            return false;
        }
        var authorities = auths();
        var targetGroupUuids = userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year).stream()
                .filter(userGroup -> userGroup.getGroup() != null)
                .map(userGroup -> userGroup.getGroup().getUuid())
                .collect(Collectors.toSet());
        return targetGroupUuids.stream()
                .anyMatch(groupUuid -> authorities.contains("GROUP:" + groupUuid + ":ANIMATOR:" + year));
    }

    public UUID requestCenterId(GroupRequest req) {
        if (req == null || req.centerUuid() == null || req.centerUuid().isBlank()) {
            return null;
        }
        return ReferenceParser.asUuid(req.centerUuid()).orElse(null);
    }

    private UUID requestCenterId(UserSalleRequest req) {
        if (req == null || req.getCenterUuid() == null || req.getCenterUuid().isBlank()) {
            return null;
        }
        return ReferenceParser.asUuid(req.getCenterUuid()).orElse(null);
    }

    private UUID requestCenterId(EventRequest req) {
        if (req == null || req.getCenterUuid() == null || req.getCenterUuid().isBlank()) {
            return null;
        }
        return ReferenceParser.asUuid(req.getCenterUuid()).orElse(null);
    }

    private UUID requestGroupId(UserSalleRequest req) {
        if (req == null || req.getGroupUuid() == null || req.getGroupUuid().isBlank()) {
            return null;
        }
        return ReferenceParser.asUuid(req.getGroupUuid()).orElse(null);
    }

    private UUID requestGroupId(com.sallejoven.backend.model.requestDto.WeeklySessionRequest req) {
        if (req == null || req.getGroupUuid() == null || req.getGroupUuid().isBlank()) {
            return null;
        }
        return ReferenceParser.asUuid(req.getGroupUuid()).orElse(null);
    }
}
