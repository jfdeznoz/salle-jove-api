package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final AcademicStateService academicStateService;
    private final GroupService groupService;
    private final EventUserService eventUserService;

    public UserGroup findActiveById(Long userGroupId) throws SalleException {
        return userGroupRepository.findByIdAndDeletedAtIsNull(userGroupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));
    }

    public List<UserGroup> findByGroupIds(Collection<Long> groupIds) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.findByGroupIdsAndYear(groupIds, year);
    }

    public List<UserGroup> findByGroupId(Long groupId) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.findByGroupIdAndYear(groupId, year);
    }

    public List<UserGroup> findByStages(Collection<Integer> stages) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.findByGroupStagesAndYear(stages, year);
    }

    public List<UserGroup> findByCenterAndStages(Long centerId, Collection<Integer> stages) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.findByCenterAndGroupStagesAndYear(centerId, stages, year);
    }

    public List<UserGroup> findByCenterAndUserTypes(Long centerId, Integer... types) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.findUserGroupsByCenterAndUserTypesAndYear(centerId, year, types);
    }

    public List<UserGroup> findActiveByCurrentYear() throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.findByYearAndDeletedAtIsNull(year);
    }

    public boolean existsActiveForUserInCurrentYear(Long userId) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.existsByUser_IdAndYearAndDeletedAtIsNull(userId, year);
    }


    public List<UserGroup> findByGroupIdsForYear(Collection<Long> groupIds, int year) {
        return userGroupRepository.findByGroupIdsAndYear(groupIds, year);
    }

    public List<UserGroup> findByGroupIdForYear(Long groupId, int year) {
        return userGroupRepository.findByGroupIdAndYear(groupId, year);
    }

    public List<UserGroup> findByStagesForYear(Collection<Integer> stages, int year) {
        return userGroupRepository.findByGroupStagesAndYear(stages, year);
    }

    public List<UserGroup> findByCenterAndStagesForYear(Long centerId, Collection<Integer> stages, int year) {
        return userGroupRepository.findByCenterAndGroupStagesAndYear(centerId, stages, year);
    }

    public List<UserGroup> findByCenterAndUserTypesForYear(Long centerId, int year, Integer... types) {
        return userGroupRepository.findUserGroupsByCenterAndUserTypesAndYear(centerId, year, types);
    }

    public List<UserGroup> findActiveByYear(int year) {
        return userGroupRepository.findByYearAndDeletedAtIsNull(year);
    }

    public List<UserGroup> findActiveCatechumensByYear(int year) {
        return userGroupRepository.findByYearAndUserTypeAndDeletedAtIsNull(year, 0);
    }

    public boolean existsActiveForUserInYear(Long userId, int year) {
        return userGroupRepository.existsByUser_IdAndYearAndDeletedAtIsNull(userId, year);
    }

    @Transactional
    public void saveAll(List<UserGroup> userGroups) {
        userGroupRepository.saveAll(userGroups);
    }

    @Transactional
    public void moveUserBetweenGroups(UserSalle user, Long fromGroupId, Long toGroupId) throws SalleException {

        if (fromGroupId.equals(toGroupId)) return;

        GroupSalle to = groupService.findById(toGroupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        // 1) localizar membership origen (user + fromGroup)
        UserGroup source = user.getGroups().stream()
                .filter(m -> m.getGroup().getId().equals(fromGroupId))
                .findFirst()
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));

        // 2) conservar tipo
        Integer type = source.getUserType();

        // 3) quitar membership origen (orphanRemoval=true la borrará en BD si tu mapeo lo tiene)
        user.getGroups().remove(source);

        // 4) crear/actualizar membership destino con mismo tipo
        UserGroup dest = user.getGroups().stream()
                .filter(m -> m.getGroup().getId().equals(toGroupId))
                .findFirst()
                .orElseGet(() -> {
                    UserGroup ug = UserGroup.builder()
                            .user(user)
                            .group(to)
                            .userType(type)
                            .year(source.getYear())
                            .build();
                    user.getGroups().add(ug);
                    return ug;
                });

        dest.setUserType(type);

       /* // 5) persistir cambios en memberships
        UserSalle updated = userService.saveUser(user);
        */

        // 6) inscribir al usuario (vía su UserGroup destino) en los eventos futuros del nuevo grupo
        eventUserService.assignFutureGroupEventsToUser(user, to);
    }

    @Transactional
    public UserGroup addUserToGroup(UserSalle user, Long groupId, int userType) throws SalleException {
        if (userType != 0 && userType != 1 && userType != 5) {
            throw new SalleException(ErrorCodes.USER_TYPE_NOT_VALID);
        }

        GroupSalle group = groupService.findById(groupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        int year = academicStateService.getVisibleYear();

        UserGroup existing = user.getGroups().stream()
                .filter(ug -> ug.getGroup() != null
                        && ug.getGroup().getId().equals(groupId)
                        && ug.getYear() != null
                        && ug.getYear().equals(year))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setUserType(userType);
            return existing;
        }

        UserGroup newUg = UserGroup.builder()
                .user(user)
                .group(group)
                .userType(userType)
                .year(year)
                .build();

        user.getGroups().add(newUg);

        eventUserService.assignFutureGroupEventsToUser(user, group);

        return newUg;
    }

    @Transactional
    public void unlinkByUserAndGroup(Long userId, Long groupId) throws SalleException {
        int year = academicStateService.getVisibleYear();

        UserGroup ug = userGroupRepository
                .findByUser_IdAndGroup_IdAndYearAndDeletedAtIsNull(userId, groupId, year)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));
        ug.setDeletedAt(LocalDateTime.now());

        eventUserService.softDeleteByUserGroupIds(List.of(ug.getId()));
    }

    @Transactional
    public void changeRoleByUserAndGroup(Long userId, Long groupId, int newUserType) throws SalleException {
        if (newUserType != 0 && newUserType != 1 && newUserType != 5) {
            throw new SalleException(ErrorCodes.USER_TYPE_NOT_VALID);
        }

        int year = academicStateService.getVisibleYear();

        UserGroup ug = userGroupRepository
                .findByUser_IdAndGroup_IdAndYearAndDeletedAtIsNull(userId, groupId, year)
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));

        ug.setUserType(newUserType);
    }

}
