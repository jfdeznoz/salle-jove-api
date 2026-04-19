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
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.AuthorityService;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAssembler {

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final AuthorityService authorityService;
    private final AcademicStateService academicStateService;
    private final UserService userService;
    private final CenterService centerService;
    private final GroupService groupService;
    private final UserGroupRepository userGroupRepository;

    public UserSelfDto toSelfDto(UserSalle user) {
        Role mainRole = authorityService.computeDisplayRole(user.getUuid());
        UserSelfDto dto = userMapper.toUserSelfDto(user);
        dto.setRol(mainRole);
        enrichWithVisibleYearAssignments(dto, user);
        return dto;
    }

    public UserSelfDto toSearchDto(UserSalle user) {
        return toSelfDto(user);
    }

    private void enrichWithVisibleYearAssignments(UserSelfDto dto, UserSalle user) {
        try {
            int year = academicStateService.getVisibleYear();
            List<UserGroup> userGroups = userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNull(user.getUuid(), year);
            if (!userGroups.isEmpty()) {
                Center firstCenter = userGroups.get(0).getGroup().getCenter();
                dto.setCenterName(firstCenter.getName());
                List<String> stageNames = userGroups.stream()
                        .map(userGroup -> userGroup.getGroup().getStage())
                        .distinct()
                        .sorted()
                        .map(String::valueOf)
                        .toList();
                dto.setGroupNames(stageNames);
            }
        } catch (Exception ignored) {
        }
    }

    public UserPendingDto toPendingDto(UserPending pending) {
        List<Role> roles = userService.getUserRoles(pending);
        Role mainRole = roles.isEmpty() ? Role.PARTICIPANT : roles.get(0);

        Center center = null;
        GroupSalle group = null;

        if (pending.getGroupUuid() != null) {
            group = groupService.findByReference(pending.getGroupUuid().toString());
            center = group.getCenter();
        } else if (pending.getCenterUuid() != null) {
            center = centerService.findByReference(pending.getCenterUuid().toString());
        }

        UserPendingDto dto = userMapper.toUserPendingDto(pending);
        dto.setRol(mainRole);
        dto.setCenter(center != null ? center.getName() + " - " + center.getCity() : null);
        dto.setStage(group != null ? group.getStage() : null);
        return dto;
    }

    public List<UserCenterGroupsDto> toUserCenterGroupsDtos(List<UserGroup> userGroups) {
        Map<Center, List<UserGroup>> byCenter = userGroups.stream()
                .collect(Collectors.groupingBy(userGroup -> userGroup.getGroup().getCenter()));

        return byCenter.entrySet().stream()
                .map(entry -> {
                    Center center = entry.getKey();
                    List<UserGroupDto> groupDtos = entry.getValue().stream()
                            .map(groupMapper::toUserGroupDto)
                            .sorted(Comparator.comparing(UserGroupDto::stage, Comparator.nullsLast(Integer::compareTo)))
                            .toList();
                    return new UserCenterGroupsDto(center.getUuid(), center.getName(), center.getCity(), groupDtos, null);
                })
                .sorted(Comparator.comparing(UserCenterGroupsDto::centerName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
