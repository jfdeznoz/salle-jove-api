package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.repository.projection.SeguroRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static java.time.LocalDateTime.now;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final AcademicStateService academicStateService;
    private final GroupService groupService;
    private final EventUserService eventUserService;
    private final UserRepository userRepository;

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

    public List<UserGroup> findByUserAndUserTypeForCurrentYear(Long userId, int userType) throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.findByUser_IdAndYearAndDeletedAtIsNullAndUserType(userId, year, userType);
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

        UserGroup source = user.getGroups().stream()
                .filter(ug -> ug.getGroup().getId().equals(fromGroupId))
                .max(Comparator.comparing(UserGroup::getYear))
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_GROUP_NOT_ASSIGNED));

        Integer year = source.getYear();
        Integer type = source.getUserType();

        GroupSalle to = groupService.findById(toGroupId);

        UserGroup dest = userGroupRepository
                .findByUser_IdAndGroup_IdAndYear(user.getId(), toGroupId, year)
                .map(ug -> {
                    if (ug.getDeletedAt() != null) ug.setDeletedAt(null);
                    return ug;
                })
                .orElseGet(() -> {
                    UserGroup ug = UserGroup.builder()
                            .user(user)       // owning side
                            .group(to)
                            .year(year)
                            .userType(type)
                            .build();
                    user.getGroups().add(ug); // inverse side (necesario para cascade)
                    return ug;
                });

        // Por si hubiera cambiado el tipo
        dest.setUserType(type);

        // 3) Soft-delete del SOURCE (no lo quites de la colección; no queremos borrar físico)
        source.setDeletedAt(now());

        // 4) Persistencia:
        //    Con cascade {PERSIST, MERGE} en UserSalle.groups te vale UNA sola save del user.
        userRepository.save(user);

        // 5) Asignar eventos futuros del nuevo grupo (idempotente con UNIQUE(event, user_group_id))
        eventUserService.assignFutureGroupEventsToUser(user, to);
    }

    @Transactional
    public UserGroup addUserToGroup(UserSalle user, Long groupId, int userType) throws SalleException {
        if (userType != 0 && userType != 1 && userType != 5) {
            throw new SalleException(ErrorCodes.USER_TYPE_NOT_VALID);
        }

        GroupSalle group = groupService.findById(groupId);

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
        ug.setDeletedAt(now());

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

    public List<SeguroRow> findSeguroRowsForCurrentYear() throws SalleException {
        int year = academicStateService.getVisibleYear();
        return userGroupRepository.findSeguroRows(year);
    }

}
