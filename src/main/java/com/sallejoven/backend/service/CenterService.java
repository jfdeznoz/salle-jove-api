package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.mapper.CenterMapper;
import com.sallejoven.backend.mapper.GroupMapper;
import com.sallejoven.backend.model.dto.CenterDto;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.enums.UserType;
import com.sallejoven.backend.model.raw.UserCenterGroupsRaw;
import com.sallejoven.backend.model.requestDto.CenterRequest;
import com.sallejoven.backend.model.requestDto.ForkCenterRequest;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CenterService {

    private final CenterRepository centerRepository;
    private final GroupService groupService;
    private final UserCenterService userCenterService;
    private final AcademicStateService academicStateService;
    private final UserGroupRepository userGroupRepository;
    private final CenterMapper centerMapper;
    private final GroupMapper groupMapper;

    public Center findById(UUID uuid) {
        return centerRepository.findById(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.CENTER_NOT_FOUND));
    }

    public Center findByReference(String reference) {
        return ReferenceParser.asUuid(reference)
                .flatMap(centerRepository::findByUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.CENTER_NOT_FOUND));
    }

    public List<Center> getAllCentersWithGroups() {
        return centerRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<CenterDto> listMyCenters(UserSalle me, List<Role> roles) {
        if (roles.contains(Role.ADMIN)) {
            return getAllCentersWithGroups().stream().map(this::toCenterDto).toList();
        }
        return userCenterService.findByUserForCurrentYear(me.getUuid()).stream()
                .map(centerMapper::fromUserCenter)
                .toList();
    }

    private CenterDto toCenterDto(Center center) {
        return centerMapper.toCenterDtoWithoutGroups(center);
    }

    @Transactional
    public CenterDto createCenter(CenterRequest request) {
        String name = normalize(request.name());
        String city = normalize(request.city());
        validateCenterUniqueness(name, city, null);
        Center saved = centerRepository.save(Center.builder().name(name).city(city).build());
        return centerMapper.toCenterDtoWithGroups(saved, List.of());
    }

    @Transactional
    public CenterDto updateCenter(UUID centerUuid, CenterRequest request) {
        Center center = findById(centerUuid);
        String name = normalize(request.name());
        String city = normalize(request.city());
        validateCenterUniqueness(name, city, centerUuid);
        center.setName(name);
        center.setCity(city);
        return toCenterDtoWithGroups(centerRepository.save(center));
    }

    @Transactional
    public void deleteCenter(UUID centerUuid) {
        Center center = findById(centerUuid);
        if (!groupService.findGroupsByCenterId(centerUuid).isEmpty()) {
            throw new SalleException(ErrorCodes.CENTER_HAS_GROUPS);
        }
        centerRepository.delete(center);
    }

    @Transactional
    public CenterDto forkCenter(UUID sourceCenterUuid, ForkCenterRequest request) {
        Center sourceCenter = findById(sourceCenterUuid);
        String newName = normalize(request.newName());
        String newCity = normalize(request.newCity());
        validateCenterUniqueness(newName, newCity, null);

        Center newCenter = centerRepository.save(Center.builder().name(newName).city(newCity).build());
        List<GroupSalle> groupsToMove = resolveGroupsToFork(sourceCenterUuid, request);

        for (GroupSalle group : groupsToMove) {
            group.setCenter(newCenter);
            groupService.saveGroup(group);
        }

        reassignUserCentersAfterFork(sourceCenterUuid, newCenter.getUuid());
        return toCenterDtoWithGroups(newCenter);
    }

    @Transactional
    public CenterDto mergeCenter(UUID targetUuid, UUID sourceUuid) {
        Center targetCenter = findById(targetUuid);
        findById(sourceUuid);
        if (targetUuid.equals(sourceUuid)) {
            return toCenterDtoWithGroups(targetCenter);
        }

        List<GroupSalle> targetGroups = new ArrayList<>(groupService.findGroupsByCenterId(targetUuid));
        List<GroupSalle> sourceGroups = new ArrayList<>(groupService.findGroupsByCenterId(sourceUuid));
        validateMergeStages(targetGroups, sourceGroups);

        for (GroupSalle group : sourceGroups) {
            group.setCenter(targetCenter);
            groupService.saveGroup(group);
        }

        List<UserCenter> sourceRoles = userCenterService.findActiveByCenterForCurrentYear(sourceUuid);
        for (UserCenter userCenter : sourceRoles) {
            ensureCenterRoleExists(userCenter.getUser().getUuid(), targetUuid, userCenter.getUserType());
        }

        userCenterService.hardDeleteAllForCenter(sourceUuid);
        centerRepository.deleteById(sourceUuid);

        List<GroupSalle> mergedGroups = new ArrayList<>(targetGroups.size() + sourceGroups.size());
        mergedGroups.addAll(targetGroups);
        mergedGroups.addAll(sourceGroups);
        return toCenterDtoWithGroups(targetCenter, mergedGroups);
    }

    public List<UserGroup> getActiveUserGroupsForYear(UserSalle user) {
        if (user == null || user.getUuid() == null) {
            return List.of();
        }
        return getActiveUserGroupsForCurrentYear(user.getUuid());
    }

    public List<UserGroup> getActiveUserGroupsForCurrentYear(UUID userUuid) {
        int visibleYear = academicStateService.getVisibleYear();
        return userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, visibleYear).stream()
                .filter(userGroup -> userGroup.getDeletedAt() == null
                        && userGroup.getGroup() != null
                        && userGroup.getYear() == visibleYear)
                .toList();
    }

    public List<GroupSalle> getGroupsForCenter(Center center) {
        return groupService.findByCenter(center);
    }

    public List<UserCenterGroupsRaw> getCentersForUserRaw(UserSalle me, List<Role> roles) {
        Role mainRole = (roles == null || roles.isEmpty()) ? Role.PARTICIPANT : roles.get(0);
        return switch (mainRole) {
            case ADMIN -> rawForAdmin();
            case GROUP_LEADER, PASTORAL_DELEGATE -> rawForLeaderOrDelegate(me);
            default -> rawForDefaultUser(me);
        };
    }

    private List<UserCenterGroupsRaw> rawForAdmin() {
        int year = academicStateService.getVisibleYear();
        List<UserCenterGroupsRaw> raws = new ArrayList<>();
        for (Center center : getAllCentersWithGroups()) {
            List<GroupSalle> groups = groupService.findGroupsByCenterIdForYear(center.getUuid(), year);
            List<UserGroupDto> dtos = groups.stream()
                    .map(group -> new UserGroupDto(UserType.ADMIN.toInt(), group.getUuid(), null, group.getStage()))
                    .toList();
            raws.add(new UserCenterGroupsRaw(center, dtos));
        }
        return raws;
    }

    private List<UserCenterGroupsRaw> rawForLeaderOrDelegate(UserSalle me) {
        List<UserCenter> myCenters = userCenterService.findByUserForCurrentYear(me.getUuid());
        Map<UUID, UserCenter> chosenByCenter = new LinkedHashMap<>();
        for (UserCenter userCenter : myCenters) {
            Integer userType = userCenter.getUserType();
            if (userType != null && (userType == 2 || userType == 3) && userCenter.getCenter() != null) {
                UUID centerUuid = userCenter.getCenter().getUuid();
                UserCenter previous = chosenByCenter.get(centerUuid);
                if (previous == null || previous.getUserType() != 3) {
                    chosenByCenter.put(centerUuid, userCenter);
                }
            }
        }

        int year = academicStateService.getVisibleYear();
        List<UserCenterGroupsRaw> result = new ArrayList<>();
        for (UserCenter userCenter : chosenByCenter.values()) {
            Center center = userCenter.getCenter();
            int roleCode = userCenter.getUserType() != null ? userCenter.getUserType() : UserType.PARTICIPANT.toInt();
            List<UserGroupDto> dtos = groupService.findGroupsByCenterIdForYear(center.getUuid(), year).stream()
                    .map(group -> new UserGroupDto(roleCode, group.getUuid(), null, group.getStage()))
                    .toList();
            result.add(new UserCenterGroupsRaw(center, dtos));
        }

        Set<UUID> mainCenterUuids = chosenByCenter.keySet();
        List<UserCenterGroupsRaw> others = rawForDefaultUser(me).stream()
                .filter(raw -> !mainCenterUuids.contains(raw.center().getUuid()))
                .toList();
        result.addAll(others);
        return result;
    }

    private List<UserCenterGroupsRaw> rawForDefaultUser(UserSalle me) {
        int year = academicStateService.getVisibleYear();
        List<UserGroup> active = userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNull(me.getUuid(), year).stream()
                .filter(userGroup -> userGroup.getGroup() != null && userGroup.getGroup().getCenter() != null)
                .toList();

        Map<Center, List<UserGroup>> byCenter = active.stream()
                .collect(Collectors.groupingBy(userGroup -> userGroup.getGroup().getCenter()));

        List<UserCenterGroupsRaw> raws = new ArrayList<>();
        for (Map.Entry<Center, List<UserGroup>> entry : byCenter.entrySet()) {
            List<UserGroupDto> dtos = entry.getValue().stream()
                    .map(userGroup -> new UserGroupDto(
                            userGroup.getUserType() != null ? userGroup.getUserType() : UserType.PARTICIPANT.toInt(),
                            userGroup.getGroup().getUuid(),
                            userGroup.getUuid(),
                            userGroup.getGroup().getStage()))
                    .toList();
            raws.add(new UserCenterGroupsRaw(entry.getKey(), dtos));
        }
        return raws;
    }

    private List<GroupSalle> resolveGroupsToFork(UUID sourceCenterUuid, ForkCenterRequest request) {
        LinkedHashSet<GroupSalle> groups = new LinkedHashSet<>();
        if (request.groupUuids() != null) {
            request.groupUuids().stream()
                    .map(groupService::findByReference)
                    .filter(group -> group.getCenter() != null && sourceCenterUuid.equals(group.getCenter().getUuid()))
                    .forEach(groups::add);
        }
        return List.copyOf(groups);
    }

    private void reassignUserCentersAfterFork(UUID sourceCenterUuid, UUID newCenterUuid) {
        for (UserCenter userCenter : userCenterService.findActiveByCenterForCurrentYear(sourceCenterUuid)) {
            UUID userUuid = userCenter.getUser().getUuid();
            if (userStillHasGroupsInCenter(userUuid, sourceCenterUuid)) {
                continue;
            }
            ensureCenterRoleExists(userUuid, newCenterUuid, userCenter.getUserType());
            userCenterService.softDelete(userCenter.getUuid());
        }
    }

    private boolean userStillHasGroupsInCenter(UUID userUuid, UUID centerUuid) {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year).stream()
                .map(UserGroup::getGroup)
                .filter(Objects::nonNull)
                .map(GroupSalle::getCenter)
                .filter(Objects::nonNull)
                .anyMatch(center -> centerUuid.equals(center.getUuid()));
    }

    private void validateMergeStages(List<GroupSalle> targetGroups, List<GroupSalle> sourceGroups) {
        Set<Integer> targetStages = targetGroups.stream()
                .map(GroupSalle::getStage)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean hasConflict = sourceGroups.stream()
                .map(GroupSalle::getStage)
                .filter(Objects::nonNull)
                .anyMatch(targetStages::contains);
        if (hasConflict) {
            throw new SalleException(ErrorCodes.MERGE_STAGE_CONFLICT);
        }
    }

    private void ensureCenterRoleExists(UUID userUuid, UUID centerUuid, Integer userType) {
        boolean alreadyExists = userCenterService.findByUserForCurrentYear(userUuid).stream()
                .anyMatch(existing -> existing.getCenter() != null
                        && centerUuid.equals(existing.getCenter().getUuid())
                        && Objects.equals(existing.getUserType(), userType));
        if (!alreadyExists) {
            userCenterService.addCenterRole(userUuid, centerUuid, userType);
        }
    }

    private CenterDto toCenterDtoWithGroups(Center center) {
        return toCenterDtoWithGroups(center, groupService.findGroupsByCenterId(center.getUuid()));
    }

    private CenterDto toCenterDtoWithGroups(Center center, List<GroupSalle> groups) {
        List<GroupDto> groupDtos = groups.stream()
                .sorted(Comparator.comparing(GroupSalle::getStage, Comparator.nullsLast(Integer::compareTo)))
                .map(groupMapper::toGroupDto)
                .toList();
        return centerMapper.toCenterDtoWithGroups(center, groupDtos);
    }

    private void validateCenterUniqueness(String name, String city, UUID excludedCenterUuid) {
        boolean exists = excludedCenterUuid == null
                ? centerRepository.existsByNameAndCity(name, city)
                : centerRepository.existsByNameAndCityAndUuidNot(name, city, excludedCenterUuid);
        if (exists) {
            throw new SalleException(ErrorCodes.CENTER_ALREADY_EXISTS);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
