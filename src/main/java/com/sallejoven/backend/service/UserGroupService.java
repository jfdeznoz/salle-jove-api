package com.sallejoven.backend.service;

import static java.time.LocalDateTime.now;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.repository.projection.SeguroRow;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final AcademicStateService academicStateService;
    private final GroupService groupService;
    private final EventUserService eventUserService;
    private final UserRepository userRepository;

    public UserGroup findActiveById(UUID userGroupUuid) {
        return userGroupRepository.findByIdAndDeletedAtIsNull(userGroupUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));
    }

    public UserGroup findById(UUID userGroupUuid) {
        return userGroupRepository.findById(userGroupUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));
    }

    public List<UserGroup> findByGroupIds(Collection<UUID> groupUuids) {
        return userGroupRepository.findByGroupUuidsAndYear(groupUuids, academicStateService.getVisibleYear());
    }

    public List<UserGroup> findByGroupId(UUID groupUuid) {
        return userGroupRepository.findByGroupUuidAndYear(groupUuid, academicStateService.getVisibleYear());
    }

    public List<UserGroup> findByStages(Collection<Integer> stages) {
        return userGroupRepository.findByGroupStagesAndYear(stages, academicStateService.getVisibleYear());
    }

    public List<UserGroup> findByCenterAndStages(UUID centerUuid, Collection<Integer> stages) {
        return userGroupRepository.findByCenterAndGroupStagesAndYear(centerUuid, stages, academicStateService.getVisibleYear());
    }

    public List<UserGroup> findByCenterAndUserTypes(UUID centerUuid, Integer... types) {
        return userGroupRepository.findUserGroupsByCenterAndUserTypesAndYear(
                centerUuid,
                academicStateService.getVisibleYear(),
                types
        );
    }

    public List<UserGroup> findActiveByCurrentYear() {
        return userGroupRepository.findByYearAndDeletedAtIsNull(academicStateService.getVisibleYear());
    }

    public boolean existsActiveForUserInCurrentYear(UUID userUuid) {
        return userGroupRepository.existsByUser_UuidAndYearAndDeletedAtIsNull(userUuid, academicStateService.getVisibleYear());
    }

    public List<UserGroup> findByGroupIdsForYear(Collection<UUID> groupUuids, int year) {
        return userGroupRepository.findByGroupUuidsAndYear(groupUuids, year);
    }

    public List<UserGroup> findByGroupIdForYear(UUID groupUuid, int year) {
        return userGroupRepository.findByGroupUuidAndYear(groupUuid, year);
    }

    public List<UserGroup> findByStagesForYear(Collection<Integer> stages, int year) {
        return userGroupRepository.findByGroupStagesAndYear(stages, year);
    }

    public List<UserGroup> findByCenterAndStagesForYear(UUID centerUuid, Collection<Integer> stages, int year) {
        return userGroupRepository.findByCenterAndGroupStagesAndYear(centerUuid, stages, year);
    }

    public List<UserGroup> findByCenterAndUserTypesForYear(UUID centerUuid, int year, Integer... types) {
        return userGroupRepository.findUserGroupsByCenterAndUserTypesAndYear(centerUuid, year, types);
    }

    public List<UserGroup> findActiveByYear(int year) {
        return userGroupRepository.findByYearAndDeletedAtIsNull(year);
    }

    public List<UserGroup> findActiveCatechumensByYear(int year) {
        return userGroupRepository.findByYearAndUserTypeAndDeletedAtIsNull(year, 0);
    }

    public List<UserGroup> findByUserAndUserTypeForCurrentYear(UUID userUuid, int userType) {
        return userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNullAndUserType(
                userUuid,
                academicStateService.getVisibleYear(),
                userType
        );
    }

    public List<UserGroup> findByUserForCurrentYear(UUID userUuid) {
        return userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNull(
                userUuid,
                academicStateService.getVisibleYear()
        );
    }

    public Map<UUID, Integer> findActiveRoleMapForGroup(UUID groupUuid, Collection<UUID> userUuids) {
        if (groupUuid == null || userUuids == null || userUuids.isEmpty()) {
            return Map.of();
        }

        return userGroupRepository.findByGroup_UuidAndYearAndDeletedAtIsNullAndUser_UuidIn(
                        groupUuid,
                        academicStateService.getVisibleYear(),
                        userUuids)
                .stream()
                .filter(userGroup -> userGroup.getUser() != null && userGroup.getUser().getUuid() != null)
                .collect(Collectors.toMap(
                        userGroup -> userGroup.getUser().getUuid(),
                        userGroup -> normalizeGroupUserType(userGroup.getUserType()),
                        (left, right) -> left
                ));
    }

    public int normalizeGroupUserType(Integer userType) {
        if (Integer.valueOf(5).equals(userType)) {
            return 1;
        }
        return userType != null ? userType : 0;
    }

    public boolean existsActiveForUserInYear(UUID userUuid, int year) {
        return userGroupRepository.existsByUser_UuidAndYearAndDeletedAtIsNull(userUuid, year);
    }

    @Transactional
    public void saveAll(List<UserGroup> userGroups) {
        userGroupRepository.saveAll(userGroups);
    }

    @Transactional
    public void moveUserBetweenGroups(UUID userUuid, UUID fromGroupUuid, UUID toGroupUuid) {
        if (fromGroupUuid.equals(toGroupUuid)) {
            return;
        }

        UserGroup source = userGroupRepository
                .findTopByUser_UuidAndGroup_UuidAndDeletedAtIsNullOrderByYearDesc(userUuid, fromGroupUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));

        Integer year = source.getYear();
        Integer type = source.getUserType();
        GroupSalle targetGroup = groupService.findById(toGroupUuid);

        UserGroup destination = userGroupRepository
                .findByUser_UuidAndGroup_UuidAndYear(userUuid, toGroupUuid, year)
                .map(userGroup -> {
                    if (userGroup.getDeletedAt() != null) {
                        userGroup.setDeletedAt(null);
                    }
                    return userGroup;
                })
                .orElseGet(() -> {
                    UserSalle user = userRepository.findById(userUuid)
                            .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
                    UserGroup userGroup = UserGroup.builder()
                            .user(user)
                            .group(targetGroup)
                            .year(year)
                            .userType(type)
                            .build();
                    return userGroupRepository.save(userGroup);
                });

        destination.setUserType(type);
        source.setDeletedAt(now());
        eventUserService.assignFutureGroupEventsToUser(destination.getUser(), targetGroup);
    }

    @Transactional
    public UserGroup addUserToGroup(UUID userUuid, UUID groupUuid, int userType) {
        int normalizedUserType = normalizeGroupUserType(userType);
        if (normalizedUserType != 0 && normalizedUserType != 1) {
            throw new SalleException(ErrorCodes.USER_TYPE_NOT_VALID);
        }

        int year = academicStateService.getVisibleYear();
        GroupSalle group = groupService.findById(groupUuid);

        UserGroup existing = userGroupRepository
                .findByUser_UuidAndGroup_UuidAndYear(userUuid, groupUuid, year)
                .orElse(null);

        if (existing != null) {
            boolean wasSoftDeleted = existing.getDeletedAt() != null;
            if (wasSoftDeleted) {
                existing.setDeletedAt(null);
            }
            existing.setUserType(normalizedUserType);
            if (wasSoftDeleted) {
                eventUserService.assignFutureGroupEventsToUser(existing.getUser(), group);
            }
            return existing;
        }

        UserSalle user = userRepository.findById(userUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));

        UserGroup newUserGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .userType(normalizedUserType)
                .year(year)
                .build();
        userGroupRepository.save(newUserGroup);

        eventUserService.assignFutureGroupEventsToUser(user, group);
        return newUserGroup;
    }

    @Transactional
    public void unlinkByUserAndGroup(UUID userUuid, UUID groupUuid) {
        int year = academicStateService.getVisibleYear();
        UserGroup userGroup = userGroupRepository
                .findByUser_UuidAndGroup_UuidAndYearAndDeletedAtIsNull(userUuid, groupUuid, year)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));
        userGroup.setDeletedAt(now());
        eventUserService.softDeleteByUserGroupIds(List.of(userGroup.getUuid()));
    }

    @Transactional
    public void changeRoleByUserAndGroup(UUID userUuid, UUID groupUuid, int newUserType) {
        int normalizedUserType = normalizeGroupUserType(newUserType);
        if (normalizedUserType != 0 && normalizedUserType != 1) {
            throw new SalleException(ErrorCodes.USER_TYPE_NOT_VALID);
        }

        int year = academicStateService.getVisibleYear();
        UserGroup userGroup = userGroupRepository
                .findByUser_UuidAndGroup_UuidAndYearAndDeletedAtIsNull(userUuid, groupUuid, year)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));

        userGroup.setUserType(normalizedUserType);
    }

    public List<SeguroRow> findSeguroRowsForCurrentYear() {
        return userGroupRepository.findSeguroRows(academicStateService.getVisibleYear());
    }
}
