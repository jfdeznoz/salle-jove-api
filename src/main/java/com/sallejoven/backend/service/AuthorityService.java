package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorityService {
    private final AcademicStateService academicStateService;
    private final UserCenterRepository userCenterRepo;
    private final UserGroupRepository userGroupRepo;

    public List<String> buildAuthoritiesForUser(Long userId, boolean isAdmin) throws SalleException {
        int year = academicStateService.getVisibleYear();
        List<String> out = new ArrayList<>();

        if (isAdmin) {
            out.add("ROLE_ADMIN");
        }

        userCenterRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year)
                .forEach(uc -> {
                    String role = (uc.getUserType() != null && uc.getUserType() == 3)
                            ? "PASTORAL_DELEGATE"
                            : "GROUP_LEADER"; // contrato: 2 o 3
                    out.add("CENTER:" + uc.getCenter().getId() + ":" + role + ":" + year);
                });

        userGroupRepo.findByUser_IdAndYearAndDeletedAtIsNullAndUserType(userId, year, 1)
                .forEach(ug -> out.add("GROUP:" + ug.getGroup().getId() + ":ANIMATOR:" + year));

        return out;
    }

    public List<String> buildContextAuthorities(Long userId) throws SalleException {
        int year = academicStateService.getVisibleYear();
        List<String> out = new ArrayList<>();

        userCenterRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year)
                .forEach(uc -> {
                    String role = (uc.getUserType() != null && uc.getUserType() == 3)
                            ? "PASTORAL_DELEGATE"
                            : "GROUP_LEADER"; // contrato: 2 o 3
                    out.add("CENTER:" + uc.getCenter().getId() + ":" + role + ":" + year);
                });

        userGroupRepo.findByUser_IdAndYearAndDeletedAtIsNullAndUserType(userId, year, 1)
                .forEach(ug -> out.add("GROUP:" + ug.getGroup().getId() + ":ANIMATOR:" + year));

        return out;
    }

    public Role computeDisplayRole(Long userId) throws SalleException {
        int year = academicStateService.getVisibleYear();

        boolean isAdmin = false;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals)) {
            isAdmin = true;
        }
        if (isAdmin) return Role.ADMIN;

        boolean hasPastoralDelegate =
                userCenterRepo.existsByUser_IdAndYearAndDeletedAtIsNullAndUserType(userId, year, 3);

        if (hasPastoralDelegate) return Role.PASTORAL_DELEGATE;

        boolean hasGroupLeader =
                userCenterRepo.existsByUser_IdAndYearAndDeletedAtIsNullAndUserType(userId, year, 2);

        if (hasGroupLeader) return Role.GROUP_LEADER;

        boolean hasAnimator =
                userGroupRepo.existsByUser_IdAndYearAndDeletedAtIsNullAndUserType(userId, year, 1);

        if (hasAnimator) return Role.ANIMATOR;

        return Role.PARTICIPANT;
    }

    public Set<String> getCurrentAuth() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return Set.of();
        return a.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    /**
     * Verifica si el usuario actual es SOLO catequista (ANIMATOR) sin otros roles administrativos.
     * Si tiene otros roles (admin, pastoralDelegate, groupLeader), retorna false.
     */
    public boolean isOnlyAnimator() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        
        Set<String> auths = a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        
        // Si es admin, no es solo catequista
        if (auths.contains("ROLE_ADMIN")) return false;
        
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;
        
        // Verificar si tiene roles de centro (PASTORAL_DELEGATE o GROUP_LEADER)
        String suffix = ":" + year;
        boolean hasCenterRole = auths.stream()
                .anyMatch(auth -> auth.startsWith("CENTER:") && auth.endsWith(suffix) &&
                        (auth.contains(":PASTORAL_DELEGATE:") || auth.contains(":GROUP_LEADER:")));
        
        if (hasCenterRole) return false; // Tiene roles de centro, no es solo catequista
        
        // Si tiene al menos un rol ANIMATOR y no tiene roles de centro ni admin, es solo catequista
        boolean hasAnimatorRole = auths.stream()
                .anyMatch(auth -> auth.startsWith("GROUP:") && auth.contains(":ANIMATOR:"));
        
        return hasAnimatorRole;
    }

    public Set<Long> extractCenterIdsForYear(Set<String> authorities, int year) {
        Set<Long> out = new java.util.HashSet<>();
        String suffix = ":" + year;
        for (String a : authorities) {
            // Ejemplos válidos:
            //  - CENTER:12:PASTORAL_DELEGATE:2025
            //  - CENTER:7:GROUP_LEADER:2025
            if (a.startsWith("CENTER:") && a.endsWith(suffix)
                    && (a.contains(":PASTORAL_DELEGATE:") || a.contains(":GROUP_LEADER:"))) {
                int first = a.indexOf(':');               // después de "CENTER"
                int second = a.indexOf(':', first + 1);   // antes del ROL
                if (second > first) {
                    try {
                        out.add(Long.parseLong(a.substring(first + 1, second)));
                    } catch (NumberFormatException ignored) { /* authority mal formada -> ignorar */ }
                }
            }
        }
        return out;
    }

    public Set<Long> extractAnimatorGroupIdsForYear(Set<String> authorities, int year) {
        Set<Long> out = new java.util.HashSet<>();
        String suffix = ":" + year;
        for (String a : authorities) {
            // Ejemplo válido: GROUP:15:ANIMATOR:2025
            if (a.startsWith("GROUP:") && a.endsWith(suffix) && a.contains(":ANIMATOR:")) {
                int first = a.indexOf(':');               // después de "GROUP"
                int second = a.indexOf(':', first + 1);   // antes de "ANIMATOR"
                if (second > first) {
                    try {
                        out.add(Long.parseLong(a.substring(first + 1, second)));
                    } catch (NumberFormatException ignored) { /* authority mal formada -> ignorar */ }
                }
            }
        }
        return out;
    }
}
