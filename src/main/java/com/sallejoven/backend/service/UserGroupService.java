package com.sallejoven.backend.service;

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

    public List<UserGroup> findByGroupIds(Collection<Long> groupIds) {
        return userGroupRepository.findByGroupIds(groupIds);
    }

    public List<UserGroup> findByStages(Collection<Integer> stages) {
        return userGroupRepository.findByGroupStages(stages);
    }

    public List<UserGroup> findByCenterAndStages(Long centerId, Collection<Integer> stages) {
        return userGroupRepository.findByCenterAndGroupStages(centerId, stages);
    }
}
