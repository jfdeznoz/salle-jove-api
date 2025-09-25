package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final AcademicStateService academicStateService;

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

    public boolean existsActiveForUserInYear(Long userId, int year) {
        return userGroupRepository.existsByUser_IdAndYearAndDeletedAtIsNull(userId, year);
    }

    @Transactional
    public void saveAll(List<UserGroup> userGroups) {
        userGroupRepository.saveAll(userGroups);
    }
}
