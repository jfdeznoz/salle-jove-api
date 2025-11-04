package com.sallejoven.backend.config.security;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.RequestEvent;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.repository.EventGroupRepository;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserPendingRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.AuthorityService;
import com.sallejoven.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Year;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component("authz")
public class AuthzBean {

    private final UserCenterRepository userCenterRepo;
    private final UserRepository userRepo;
    private final UserGroupRepository userGroupRepo;
    private final GroupRepository groupRepository;
    private final AcademicStateService academicStateService;
    private final EventService eventService;
    private final EventGroupRepository eventGroupRepo;
    private final AuthorityService authorityService;
    private final UserPendingRepository userPendingRepo;

    private Set<String> auths() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return Set.of();
        return a.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private boolean isAdmin(Set<String> auths) { return auths.contains("ROLE_ADMIN"); }

    private boolean isSelf(Long userId) {
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        String me = a.getName();
        return userRepo.findById(userId).map(UserSalle::getEmail).map(me::equalsIgnoreCase).orElse(false);
    }

    public boolean isAnyManagerType() {
        var as = auths();
        if (isAdmin(as)) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        String suffix = ":" + year;
        for (String a : as) {
            if (a.startsWith("CENTER:") && a.endsWith(suffix) &&
                    (a.contains(":PASTORAL_DELEGATE:") || a.contains(":GROUP_LEADER:"))) {
                return true;
            }
        }
        return false;
    }

    public boolean canViewUserGroups(Long userId) throws SalleException {
        Set<String> as = auths();
        if (isAdmin(as) || isSelf(userId)) return true;

        int year = academicStateService.getVisibleYear();

        var ugs = userGroupRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year);

        var targetCenterIds = ugs.stream()
                .filter(ug -> ug.getGroup() != null && ug.getGroup().getCenter() != null)
                .map(ug -> ug.getGroup().getCenter().getId())
                .collect(Collectors.toSet());

        userCenterRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year)
                .forEach(uc -> {
                    if (uc.getCenter() != null) targetCenterIds.add(uc.getCenter().getId());
                });

        for (Long cid : targetCenterIds) {
            if (as.contains("CENTER:" + cid + ":PASTORAL_DELEGATE:" + year)
                    || as.contains("CENTER:" + cid + ":GROUP_LEADER:" + year)) {
                return true;
            }
        }

        var targetGroupIds = ugs.stream()
                .filter(ug -> ug.getGroup() != null)
                .map(ug -> ug.getGroup().getId())
                .collect(Collectors.toSet());

        for (Long gid : targetGroupIds) {
            if (as.contains("GROUP:" + gid + ":ANIMATOR:" + year)) {
                return true;
            }
        }

        return false;
    }

    public boolean canViewUserCenters(Long userId) {
        var as = auths();                        // authorities del llamante
        if (isAdmin(as) || isSelf(userId)) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        // 1) Centros del llamante (año visible) desde sus authorities "CENTER:{cid}:{ROL}:{year}"
        var viewerCenters = authorityService.extractCenterIdsForYear(as, year);
        if (viewerCenters.isEmpty()) return false; // no tiene ningún centro con rol de centro

        // 2) Centros del usuario objetivo (año visible): unión de sus UserCenter + UserGroup.center
        var targetCenters = new java.util.HashSet<Long>();

        userCenterRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year)
                .forEach(uc -> {
                    if (uc.getCenter() != null) targetCenters.add(uc.getCenter().getId());
                });

        userGroupRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year)
                .forEach(ug -> {
                    if (ug.getGroup() != null && ug.getGroup().getCenter() != null)
                        targetCenters.add(ug.getGroup().getCenter().getId());
                });

        // 3) Intersección → permiso
        for (Long cid : targetCenters) {
            if (viewerCenters.contains(cid)) return true;
        }
        // Si el usuario objetivo no tiene nada, no hay intersección → 403 (el front verá lista vacía si pregunta por sí mismo o admin)
        return false;
    }


    public boolean canManageCenterAsDelegate(Long centerId) {
        var as = auths();
        if (isAdmin(as)) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        return as.contains("CENTER:" + centerId + ":PASTORAL_DELEGATE:" + year);
    }

    public boolean canManageUserCenterAsDelegate(Long userCenterId) {
        var as = auths();
        if (isAdmin(as)) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        return userCenterRepo.findById(userCenterId)
                .map(uc -> uc.getCenter() != null ? uc.getCenter().getId() : null)
                .map(cid -> as.contains("CENTER:" + cid + ":PASTORAL_DELEGATE:" + year))
                .orElse(false);
    }

    public boolean hasCenterRole(Long centerId, String... roles) {
        var auths = auths();
        if (isAdmin(auths)) return true;
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        Set<String> set = a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        int year = Year.now().getValue(); // o inyecta AcademicStateService si prefieres el año visible
        for (String r : roles) {
            if (set.contains("CENTER:" + centerId + ":" + r + ":" + year)) return true;
        }
        return false;
    }

    public boolean hasGroupRole(Long groupId, String... roles) {
        var auths = auths();
        if (isAdmin(auths)) return true;
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        Set<String> set = a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        int year = Year.now().getValue(); // o AcademicStateService
        for (String r : roles) {
            if (set.contains("GROUP:" + groupId + ":" + r + ":" + year)) return true;
        }
        return false;
    }

    public boolean hasCenterOfGroup(Long groupId, String... roles) {
        var as = auths();
        if (as.contains("ROLE_ADMIN")) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        var gOpt = groupRepository.findById(groupId);
        if (gOpt.isEmpty() || gOpt.get().getCenter() == null) return false;

        Long centerId = gOpt.get().getCenter().getId();
        for (String r : roles) {
            if (as.contains("CENTER:" + centerId + ":" + r + ":" + year)) return true;
        }
        return false;
    }

    public boolean canCreateEvent(RequestEvent req) {
        var as = auths();
        if (isAdmin(as)) return true;

        if (req == null) return false;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        // Eventos generales: solo admin (cámbialo a isAnyManagerType() si quieres permitir PD/GL globalmente)
        if (Boolean.TRUE.equals(req.getIsGeneral())) {
            return false;
        }

        // Evento local: debe venir centerId y el usuario debe ser PD o GL de ese centro en el año visible
        Long centerId = req.getCenterId();
        if (centerId == null) return false;

        return as.contains("CENTER:" + centerId + ":PASTORAL_DELEGATE:" + year)
                || as.contains("CENTER:" + centerId + ":GROUP_LEADER:" + year);
    }

    public boolean canManageEventForEditOrDelete(Long eventId) {
        var as = auths();
        if (isAdmin(as)) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        var evOpt = eventService.findById(eventId);
        if (evOpt.isEmpty()) return false;

        var ev = evOpt.get();
        if (Boolean.TRUE.equals(ev.getIsGeneral())) return false;

        var centerIds = eventGroupRepo.findByEventId(eventId).stream()
                .filter(eg -> eg.getGroupSalle() != null && eg.getGroupSalle().getCenter() != null)
                .map(eg -> eg.getGroupSalle().getCenter().getId())
                .collect(Collectors.toSet());

        if (centerIds.isEmpty()) return false; // evento no general sin centros asociados -> denegar

        for (Long cid : centerIds) {
            if (as.contains("CENTER:" + cid + ":PASTORAL_DELEGATE:" + year)
                    || as.contains("CENTER:" + cid + ":GROUP_LEADER:" + year)) {
                return true;
            }
        }
        return false;
    }

    public boolean canManageEvent(Long eventId) {
        var as = auths();
        if (isAdmin(as)) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        var egs = eventGroupRepo.findByEventId(eventId);
        var centerIds = egs.stream()
                .filter(eg -> eg.getGroupSalle()!=null && eg.getGroupSalle().getCenter()!=null)
                .map(eg -> eg.getGroupSalle().getCenter().getId())
                .collect(java.util.stream.Collectors.toSet());

        // Pase por centro (PD/GL)
        for (Long cid : centerIds) {
            if (as.contains("CENTER:" + cid + ":PASTORAL_DELEGATE:" + year)
                    || as.contains("CENTER:" + cid + ":GROUP_LEADER:" + year)) {
                return true;
            }
        }

        // Pase por catequista (BD)
        Long meId = currentUserIdOrNull();
        if (meId != null) {
            for (var eg : egs) {
                var g = eg.getGroupSalle();
                if (g != null && userGroupRepo
                        .existsByUser_IdAndGroup_IdAndYearAndDeletedAtIsNullAndUserType(meId, g.getId(), year, 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Long currentUserIdOrNull() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        String email = a.getName();
        return userRepo.findByEmail(email).map(UserSalle::getId).orElse(null);
    }

    public boolean canManageEventGroupParticipants(Long eventId, Long groupId) {
        var as = auths();
        if (isAdmin(as)) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        boolean groupIsInEvent = eventGroupRepo.findByEventId(eventId).stream()
                .anyMatch(eg -> eg.getGroupSalle() != null && groupId.equals(eg.getGroupSalle().getId()));
        if (!groupIsInEvent) return false;

        Long meId = currentUserIdOrNull();
        if (meId != null && userGroupRepo
                .existsByUser_IdAndGroup_IdAndYearAndDeletedAtIsNullAndUserType(meId, groupId, year, 1)) {
            return true;
        }

        Long centerId = eventGroupRepo.findByEventId(eventId).stream()
                .filter(eg -> eg.getGroupSalle() != null && groupId.equals(eg.getGroupSalle().getId()))
                .map(eg -> eg.getGroupSalle().getCenter())
                .filter(Objects::nonNull)
                .map(Center::getId)
                .findFirst()
                .orElse(null);

        if (centerId == null) return false;

        return as.contains("CENTER:" + centerId + ":PASTORAL_DELEGATE:" + year)
                || as.contains("CENTER:" + centerId + ":GROUP_LEADER:" + year);
    }

    private static String normalizeRole(String raw) {
        if (raw == null || raw.isBlank()) return "ROLE_PARTICIPANT";
        String r = raw.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }

    public boolean canModeratePending(Long pendingId) {
        var as = auths();
        if (isAdmin(as)) return true;

        UserPending p = userPendingRepo.findById(pendingId).orElse(null);
        if (p == null) return false;

        String role = normalizeRole(p.getRoles());
        Long centerId = p.getCenterId();
        Long groupId  = p.getGroupId();

        if (centerId != null) {
            if (Objects.equals(role, "ROLE_PASTORAL_DELEGATE")) {
                return false;
            }
            return hasCenterRole(centerId, "PASTORAL_DELEGATE");
        }

        if (groupId != null) {
            GroupSalle g = groupRepository.findById(groupId).orElse(null);
            if (g == null || g.getCenter() == null) return false;
            Long cid = g.getCenter().getId();
            return hasCenterRole(cid, "PASTORAL_DELEGATE", "GROUP_LEADER");
        }

        return false;
    }

    public boolean canCreateUser(UserSalleRequest req) {
        var as = auths();
        if (isAdmin(as)) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null || req == null) return false;

        // normalizar rol solicitado
        String role = normalizeRole(req.getRol()); // devuelve "ROLE_*"

        // 1) Caso por centro: solo PD y únicamente para crear GROUP_LEADER
        if (req.getCenterId() != null) {
            Long centerId = req.getCenterId().longValue();
            if (!"ROLE_GROUP_LEADER".equals(role)) {
                // no permitimos que un PD cree otro PD u otros roles por centro
                return false;
            }
            // PD del centro correspondiente
            return auths().contains("CENTER:" + centerId + ":PASTORAL_DELEGATE:" + year);
        }

        // 2) Caso por grupo: PD o GL del centro del grupo
        if (req.getGroupId() != null) {
            Long groupId = req.getGroupId().longValue();
            GroupSalle g = groupRepository.findById(groupId).orElse(null);
            if (g == null || g.getCenter() == null) return false;
            Long cid = g.getCenter().getId();

            return as.contains("CENTER:" + cid + ":PASTORAL_DELEGATE:" + year)
                    || as.contains("CENTER:" + cid + ":GROUP_LEADER:" + year);
        }

        // 3) Si no hay ni centerId ni groupId: no autorizado (salvo admin arriba)
        return false;
    }

    public boolean canManageUser(Long userId) {
        var as = auths();
        if (isAdmin(as)) return true;

        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        // Centros del usuario objetivo en el año visible: por UserGroup y por UserCenter
        var targetCenters = new java.util.HashSet<Long>();

        userGroupRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year)
                .forEach(ug -> {
                    if (ug.getGroup() != null && ug.getGroup().getCenter() != null)
                        targetCenters.add(ug.getGroup().getCenter().getId());
                });

        userCenterRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year)
                .forEach(uc -> {
                    if (uc.getCenter() != null)
                        targetCenters.add(uc.getCenter().getId());
                });

        // El llamante debe ser PD o GL de al menos uno de esos centros
        for (Long cid : targetCenters) {
            if (as.contains("CENTER:" + cid + ":PASTORAL_DELEGATE:" + year)
                    || as.contains("CENTER:" + cid + ":GROUP_LEADER:" + year)) {
                return true;
            }
        }
        return false;
    }

    public boolean canEditUserAsAnimator(Long userId) {
        var as = auths();
        Integer year = academicStateService.getVisibleYearOrNull();
        if (year == null) return false;

        var targetGroupIds = userGroupRepo.findByUser_IdAndYearAndDeletedAtIsNull(userId, year)
                .stream()
                .filter(ug -> ug.getGroup() != null)
                .map(ug -> ug.getGroup().getId())
                .collect(Collectors.toSet());

        if (targetGroupIds.isEmpty()) return false;

        for (Long gid : targetGroupIds) {
            if (as.contains("GROUP:" + gid + ":ANIMATOR:" + year)) {
                return true;
            }
        }
        return false;
    }

}
