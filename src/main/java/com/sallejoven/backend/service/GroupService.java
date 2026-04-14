package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final CenterRepository centerRepository;
    private final UserGroupRepository userGroupRepository;
    private final EventGroupService eventGroupService;
    private final AuthorityService authorityService;
    private final AcademicStateService academicStateService;

    public GroupSalle findById(UUID uuid) {
        return groupRepository.findById(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
    }

    public GroupSalle findByReference(String reference) {
        return ReferenceParser.asUuid(reference)
                .flatMap(groupRepository::findByUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
    }

    public List<GroupSalle> findAll() {
        return groupRepository.findAll();
    }

    public Set<GroupSalle> findAllByIds(List<UUID> uuids) {
        return new HashSet<>(groupRepository.findAllById(uuids));
    }

    public List<GroupSalle> findByCenter(Center center) {
        return groupRepository.findByCenter(center);
    }

    public List<GroupSalle> findGroupsByCenterId(UUID centerUuid) {
        return groupRepository.findByCenterUuid(centerUuid);
    }

    public List<GroupSalle> findGroupsByCenterIdForYear(UUID centerUuid, int year) {
        List<UUID> groupUuids = userGroupRepository.findDistinctGroupUuidsByCenterUuidAndYear(centerUuid, year);
        return groupUuids.isEmpty() ? List.of() : groupRepository.findAllById(groupUuids);
    }

    public List<GroupSalle> findAllByEvent(UUID eventUuid) {
        int year = academicStateService.getVisibleYear();
        Set<String> auths = authorityService.getCurrentAuth();

        if (auths.contains("ROLE_ADMIN")) {
            return eventGroupService.getEventGroupsByEventId(eventUuid).stream()
                    .map(EventGroup::getGroupSalle)
                    .distinct()
                    .toList();
        }

        var centerUuids = authorityService.extractCenterIdsForYear(auths, year);
        var groupsByCenter = centerUuids.isEmpty()
                ? List.<GroupSalle>of()
                : eventGroupService.getEventGroupsByEventAndCenters(eventUuid, centerUuids.stream().toList()).stream()
                        .map(EventGroup::getGroupSalle)
                        .toList();

        var animatorGroupUuids = authorityService.extractAnimatorGroupIdsForYear(auths, year);
        var groupsByAnimator = animatorGroupUuids.isEmpty()
                ? List.<GroupSalle>of()
                : eventGroupService.getEventGroupsByEventIdAndGroupIds(eventUuid, animatorGroupUuids.stream().toList()).stream()
                        .map(EventGroup::getGroupSalle)
                        .toList();

        return Stream.concat(groupsByCenter.stream(), groupsByAnimator.stream())
                .distinct()
                .toList();
    }

    public List<GroupSalle> findAllByStages(List<Integer> stages) {
        return groupRepository.findByStageIn(stages);
    }

    public List<GroupSalle> findAllByStagesAndCenter(List<Integer> stages, UUID centerUuid) {
        return groupRepository.findAllByStagesAndCenterUuid(stages, centerUuid);
    }

    public GroupSalle createGroup(UUID centerUuid, Integer stage) {
        Center center = centerRepository.findById(centerUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.CENTER_NOT_FOUND));
        GroupSalle group = GroupSalle.builder()
                .center(center)
                .stage(stage)
                .build();
        return groupRepository.save(group);
    }

    public GroupSalle saveGroup(GroupSalle group) {
        return groupRepository.save(group);
    }

    public void deleteGroup(UUID uuid) {
        groupRepository.deleteById(uuid);
    }

    public GroupSalle getByCenterAndStageOrThrow(UUID centerUuid, int stage) {
        return groupRepository.findByCenterUuidAndStage(centerUuid, stage)
                .orElseThrow(() -> new SalleException(ErrorCodes.PROMOTION_TARGET_GROUP_NOT_FOUND));
    }

    public List<GroupSalle> findEffectiveGroupsForUser(UserSalle user) {
        int year = academicStateService.getVisibleYear();
        Set<String> auths = authorityService.getCurrentAuth();

        var centerUuids = authorityService.extractCenterIdsForYear(auths, year);
        List<GroupSalle> groupsByCenters = centerUuids.isEmpty()
                ? List.of()
                : groupRepository.findByCenterUuidIn(centerUuids.stream().toList());

        List<GroupSalle> userActiveGroups = user.getGroups() == null ? List.of()
                : user.getGroups().stream()
                        .filter(this::isActive)
                        .filter(userGroup -> userGroup.getYear() != null && userGroup.getYear() == year)
                        .map(UserGroup::getGroup)
                        .filter(Objects::nonNull)
                        .toList();

        return Stream.concat(groupsByCenters.stream(), userActiveGroups.stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private boolean isActive(UserGroup userGroup) {
        return userGroup != null && userGroup.getDeletedAt() == null && userGroup.getGroup() != null;
    }
}
