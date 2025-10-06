package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.UserCenterDto;
import com.sallejoven.backend.model.dto.UserCenterGroupsDto;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.enums.UserType;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.utils.SalleConverters;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;
    private final GroupService groupService;
    private final UserCenterService userCenterService;
    private final SalleConverters salleConverters;

    public List<Center> getAllCentersWithGroups() {
        return centerRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<GroupSalle> getGroupsForCenter(Center center) {
        return groupService.findByCenter(center);
    }

    public List<UserCenterGroupsDto> getCentersForUser(UserSalle me, List<Role> roles) throws SalleException {
        Role mainRole = (roles == null || roles.isEmpty()) ? Role.PARTICIPANT : Collections.min(roles);

        switch (mainRole) {
            case ADMIN:
                return buildForAdmin();

            case GROUP_LEADER:
            case PASTORAL_DELEGATE:
                return  buildForLeaderOrDelegate(me);

            default:
                return buildForDefaultUser(me);
        }
    }

    private List<UserCenterGroupsDto> buildForAdmin() {
        List<UserCenterGroupsDto> dtos = new ArrayList<>();
        for (Center c : getAllCentersWithGroups()) {
            List<GroupSalle> groups = groupService.findGroupsByCenterId(c.getId());
            dtos.add(salleConverters.toUserCenterGroupsDto(c, groups, UserType.ADMIN.toInt())); // 4
        }
        return dtos;
    }

    private List<UserCenterGroupsDto> buildForLeaderOrDelegate(UserSalle me) throws SalleException {
        List<UserCenterGroupsDto> result = new ArrayList<>();

        UserCenter myCenter = userCenterService.findByUserForCurrentYear(me.getId());
        final Integer mainCenterIdInt;

        if (myCenter != null) {
            Center c = myCenter.getCenter();
            mainCenterIdInt = Math.toIntExact(c.getId()); // asignación única

            List<GroupSalle> groups = groupService.findGroupsByCenterId(c.getId());
            result.add(salleConverters.toUserCenterGroupsDto(c, groups, myCenter.getUserType())); // 2 ó 3
        } else {
            mainCenterIdInt = null;
        }

        List<UserCenterGroupsDto> allUserCenters = buildForDefaultUser(me);

        if (mainCenterIdInt != null) {
            allUserCenters.removeIf(dto -> Objects.equals(dto.getCenterId(), mainCenterIdInt));
        }

        result.addAll(allUserCenters);
        return result;
    }


    private List<UserCenterGroupsDto> buildForDefaultUser(UserSalle me) {
        List<UserGroup> active = me.getGroups().stream()
                .filter(this::isActive)
                .toList();
        return salleConverters.userGroupsToUserCenters(active);
    }

    private boolean isActive(UserGroup ug) {
        return ug != null && ug.getDeletedAt() == null && ug.getGroup() != null;
    }

    public UserCenter getUserCenterDtoForCurrentYear(Long userId) throws SalleException {
        return userCenterService.findByUserForCurrentYear(userId);
    }

}