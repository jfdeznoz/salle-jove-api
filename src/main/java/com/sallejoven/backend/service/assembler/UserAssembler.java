package com.sallejoven.backend.service.assembler;

import com.sallejoven.backend.mapper.GroupMapper;
import com.sallejoven.backend.mapper.UserMapper;
import com.sallejoven.backend.model.dto.UserCenterGroupsDto;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.dto.UserPendingDto;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.service.AuthorityService;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAssembler {

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final AuthorityService authorityService;
    private final UserService userService;
    private final CenterService centerService;
    private final GroupService groupService;

    public UserSelfDto toSelfDto(UserSalle user) {
        Role mainRole = authorityService.computeDisplayRole(user.getId());
        UserSelfDto dto = userMapper.toUserSelfDto(user);
        dto.setRol(mainRole);
        return dto;
    }

    public UserPendingDto toPendingDto(UserPending pending) {
        List<Role> roles = userService.getUserRoles(pending);
        Role mainRole = roles.isEmpty() ? Role.PARTICIPANT : roles.getFirst();

        Center center = null;
        GroupSalle group = null;

        if (pending.getGroupId() != null) {
            group = groupService.findById(pending.getGroupId());
            center = group.getCenter();
        } else if (pending.getCenterId() != null) {
            center = centerService.findById(pending.getCenterId());
        }

        UserPendingDto dto = userMapper.toUserPendingDto(pending);
        dto.setRol(mainRole);
        dto.setCenter(center != null ? center.getName() + " - " + center.getCity() : null);
        dto.setStage(group != null ? group.getStage() : null);
        return dto;
    }

    public List<UserCenterGroupsDto> toUserCenterGroupsDtos(List<UserGroup> userGroups) {
        Map<Center, List<UserGroup>> byCenter = userGroups.stream()
                .collect(Collectors.groupingBy(ug -> ug.getGroup().getCenter()));

        return byCenter.entrySet().stream()
                .map(entry -> {
                    Center c = entry.getKey();
                    List<UserGroupDto> groupDtos = entry.getValue().stream()
                            .map(groupMapper::toUserGroupDto)
                            .sorted(Comparator.comparing(UserGroupDto::stage, Comparator.nullsLast(Integer::compareTo)))
                            .toList();
                    return new UserCenterGroupsDto(c.getId(), c.getName(), c.getCity(), groupDtos);
                })
                .sorted(Comparator.comparing(UserCenterGroupsDto::centerName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
