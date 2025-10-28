package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final EventGroupService eventGroupService;
    private final AuthorityService authorityService;
    private final AcademicStateService academicStateService;

    public GroupSalle findById(Long id) throws SalleException {
        return groupRepository.findById(id)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
    }

    public List<GroupSalle> findAll() {
        return groupRepository.findAll();
    }

    public Set<GroupSalle> findAllByIds(List<Long> ids) {
        return new HashSet<>(groupRepository.findAllById(ids));
    }

    public List<GroupSalle> findByCenter(Center center) {
        return groupRepository.findByCenter(center);
    }

    public List<GroupSalle> findGroupsByCenterId(Long centerId) {
        return groupRepository.findByCenterId(centerId);
    }

    public List<GroupSalle> findAllByEvent(Long eventId) throws SalleException {
        int year = academicStateService.getVisibleYear();
        Set<String> auths = authorityService.getCurrentAuth();

        // 1) ADMIN -> todos los grupos del evento
        if (auths.contains("ROLE_ADMIN")) {
            return eventGroupService.getEventGroupsByEventId(eventId).stream()
                    .map(EventGroup::getGroupSalle)
                    .distinct()
                    .toList();
        }

        // 2) PD/GL -> grupos del evento de sus centros
        var centerIds = authorityService.extractCenterIdsForYear(auths, year);
        var groupsByCenter = centerIds.isEmpty()
                ? List.<GroupSalle>of()
                : eventGroupService.getEventGroupsByEventAndCenters(eventId, centerIds.stream().toList())
                .stream()
                .map(EventGroup::getGroupSalle)
                .toList();

        // 3) ANIMATOR -> grupos del evento donde es catequista (según authorities GROUP:{gid}:ANIMATOR:{year})
        var animatorGroupIds = authorityService.extractAnimatorGroupIdsForYear(auths, year);
        var groupsByAnimator = animatorGroupIds.isEmpty()
                ? List.<GroupSalle>of()
                : eventGroupService.getEventGroupsByEventIdAndGroupIds(eventId, animatorGroupIds.stream().toList())
                .stream()
                .map(EventGroup::getGroupSalle)
                .toList();

        // 4) Unión (sin duplicados)
        return java.util.stream.Stream.concat(groupsByCenter.stream(), groupsByAnimator.stream())
                .distinct()
                .toList();
    }

    public List<GroupSalle> findAllByStages(List<Integer> stages) {
        return groupRepository.findByStageIn(stages);
    }

    public List<GroupSalle> findAllByStagesAndCenter(List<Integer> stages, Long centerId) {
        return groupRepository.findAllByStagesAndCenterId(stages, centerId);
    }

    public GroupSalle saveGroup(GroupSalle group) {
        return groupRepository.save(group);
    }

    public void deleteGroup(Long id) {
        groupRepository.deleteById(id);
    }

    public GroupSalle getByCenterAndStageOrThrow(Long centerId, int stage) throws SalleException {
        return groupRepository.findByCenterIdAndStage(centerId, stage)
                .orElseThrow(() -> new SalleException(ErrorCodes.PROMOTION_TARGET_GROUP_NOT_FOUND));
    }

    public List<GroupSalle> findEffectiveGroupsForUser(UserSalle user) throws SalleException {
        int year = academicStateService.getVisibleYear();
        Set<String> auths = authorityService.getCurrentAuth();

        // 1) Grupos por centros si es PD/GL
        var centerIds = authorityService.extractCenterIdsForYear(auths, year); // ya lo usas en findAllByEvent
        List<GroupSalle> groupsByCenters = centerIds.isEmpty()
                ? List.of()
                : groupRepository.findByCenterIdIn(centerIds.stream().toList());

        // 2) Grupos activos del usuario en el año visible
        List<GroupSalle> userActiveGroups = user.getGroups() == null ? List.of()
                : user.getGroups().stream()
                .filter(this::isActive)               // deletedAt == null
                .filter(ug -> ug.getYear() != null && ug.getYear() == year)
                .map(UserGroup::getGroup)
                .filter(Objects::nonNull)
                .toList();

        // 3) Unión sin duplicados
        return Stream.concat(groupsByCenters.stream(), userActiveGroups.stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private boolean isActive(UserGroup ug) {
        return ug != null && ug.getDeletedAt() == null && ug.getGroup() != null;
    }
}
