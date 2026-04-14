package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRoleHelper {

    private final AcademicStateService academicStateService;

    public String normalizeRole(String rolText) {
        String r = (rolText == null || rolText.isBlank()) ? "PARTICIPANT" : rolText.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }

    public boolean usesCenterOnly(String role) {
        return role.endsWith("GROUP_LEADER") || role.endsWith("PASTORAL_DELEGATE");
    }

    public boolean isAnimator(String role) {
        return role.endsWith("ANIMATOR");
    }

    public int mapUserTypeFromRequest(String role) {
        String r = role.startsWith("ROLE_") ? role.substring(5) : role;
        return switch (r) {
            case "GROUP_LEADER"      -> 2;
            case "PASTORAL_DELEGATE" -> 3;
            case "ANIMATOR"          -> 1;
            default                  -> 0;
        };
    }

    /**
     * Valida que la request contiene el contexto mínimo para materializar el rol solicitado.
     * - Roles de centro (GROUP_LEADER, PASTORAL_DELEGATE) requieren centerUuid.
     * - Roles de grupo (ANIMATOR, PARTICIPANT) requieren groupUuid.
     * ADMIN no requiere contexto adicional.
     *
     * Falla rápido con SalleException para evitar persistir usuarios huérfanos sin rol asignado.
     */
    public void assertScopeForRole(String role, UUID centerUuid, UUID groupUuid) {
        final boolean isAdmin = "ROLE_ADMIN".equals(role);
        if (isAdmin) return;

        if (usesCenterOnly(role)) {
            if (centerUuid == null) {
                throw new SalleException(ErrorCodes.CENTER_NOT_FOUND,
                        "Se requiere centerUuid para crear un usuario con rol " + role);
            }
            return;
        }

        // Resto (PARTICIPANT, ANIMATOR): se materializan vía grupo.
        if (groupUuid == null) {
            throw new SalleException(ErrorCodes.GROUP_NOT_FOUND,
                    "Se requiere groupUuid para crear un usuario con rol " + role);
        }
    }

    public UserGroup ensureMembership(UserSalle user, GroupSalle group, int userType) {
        final int year = academicStateService.getVisibleYear();

        UserGroup existing = user.getGroups().stream()
                .filter(ug -> ug.getGroup() != null
                        && ug.getGroup().getId().equals(group.getId())
                        && ug.getDeletedAt() == null
                        && ug.getYear() != null
                        && ug.getYear() == year)
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setUserType(userType);
            return existing;
        }

        UserGroup membership = UserGroup.builder()
                .user(user)
                .group(group)
                .userType(userType)
                .year(year)
                .build();

        user.getGroups().add(membership);
        return membership;
    }
}
