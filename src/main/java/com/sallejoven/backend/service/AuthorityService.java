package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorityService {

    private final AcademicStateService academicStateService;
    private final UserRepository userRepository;
    private final UserCenterRepository userCenterRepository;
    private final UserGroupRepository userGroupRepository;

    public Set<String> buildAuthoritiesForUser(UUID userUuid, boolean isAdmin) {
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        if (isAdmin) {
            authorities.add("ROLE_ADMIN");
        }
        authorities.addAll(buildContextAuthorities(userUuid));
        if (authorities.isEmpty()) {
            authorities.add("ROLE_PARTICIPANT");
        }
        return authorities;
    }

    public Set<String> buildContextAuthorities(UUID userUuid) {
        int year = academicStateService.getVisibleYear();
        LinkedHashSet<String> authorities = new LinkedHashSet<>();

        userCenterRepository.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year)
                .forEach(userCenter -> {
                    UUID centerUuid = userCenter.getCenter() != null ? userCenter.getCenter().getUuid() : null;
                    Integer userType = userCenter.getUserType();
                    if (centerUuid == null || userType == null) {
                        return;
                    }
                    if (userType == 3) {
                        authorities.add("CENTER:" + centerUuid + ":PASTORAL_DELEGATE:" + year);
                    } else if (userType == 2) {
                        authorities.add("CENTER:" + centerUuid + ":GROUP_LEADER:" + year);
                    }
                });

        userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNullAndUserType(userUuid, year, 1)
                .forEach(userGroup -> {
                    UUID groupUuid = userGroup.getGroup() != null ? userGroup.getGroup().getUuid() : null;
                    if (groupUuid != null) {
                        authorities.add("GROUP:" + groupUuid + ":ANIMATOR:" + year);
                    }
                });

        return authorities;
    }

    public Role computeDisplayRole(UUID userUuid) {
        UserSalle user = userRepository.findById(userUuid).orElse(null);
        if (user != null && Boolean.TRUE.equals(user.getIsAdmin())) {
            return Role.ADMIN;
        }

        int year = academicStateService.getVisibleYear();
        boolean hasPastoralDelegate = userCenterRepository
                .existsByUser_UuidAndYearAndDeletedAtIsNullAndUserType(userUuid, year, 3);
        if (hasPastoralDelegate) {
            return Role.PASTORAL_DELEGATE;
        }

        boolean hasGroupLeader = userCenterRepository
                .existsByUser_UuidAndYearAndDeletedAtIsNullAndUserType(userUuid, year, 2);
        if (hasGroupLeader) {
            return Role.GROUP_LEADER;
        }

        boolean hasAnimator = userGroupRepository
                .existsByUser_UuidAndYearAndDeletedAtIsNullAndUserType(userUuid, year, 1);
        if (hasAnimator) {
            return Role.ANIMATOR;
        }

        return Role.PARTICIPANT;
    }

    public Set<UUID> extractCenterIdsForYear(Set<String> auths, int year) {
        return auths.stream()
                .filter(auth -> auth.startsWith("CENTER:"))
                .map(auth -> auth.split(":"))
                .filter(parts -> parts.length == 4)
                .filter(parts -> String.valueOf(year).equals(parts[3]))
                .filter(parts -> "PASTORAL_DELEGATE".equals(parts[2]) || "GROUP_LEADER".equals(parts[2]))
                .map(parts -> parseUuid(parts[1]))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<UUID> extractAnimatorGroupIdsForYear(Set<String> auths, int year) {
        return auths.stream()
                .filter(auth -> auth.startsWith("GROUP:"))
                .map(auth -> auth.split(":"))
                .filter(parts -> parts.length == 4)
                .filter(parts -> String.valueOf(year).equals(parts[3]))
                .filter(parts -> "ANIMATOR".equals(parts[2]))
                .map(parts -> parseUuid(parts[1]))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getCurrentAuth() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isOnlyAnimator() {
        Set<String> auths = getCurrentAuth();
        if (auths.contains("ROLE_ADMIN")) {
            return false;
        }
        int year = academicStateService.getVisibleYear();
        boolean hasAnimator = !extractAnimatorGroupIdsForYear(auths, year).isEmpty();
        boolean hasCenterRole = !extractCenterIdsForYear(auths, year).isEmpty();
        return hasAnimator && !hasCenterRole;
    }

    private UUID parseUuid(String raw) {
        return UUID.fromString(raw);
    }
}
