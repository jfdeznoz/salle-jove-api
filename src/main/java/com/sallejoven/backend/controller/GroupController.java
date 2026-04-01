package com.sallejoven.backend.controller;

import com.sallejoven.backend.mapper.GroupMapper;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.dto.GroupResponse;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.UserType;
import com.sallejoven.backend.model.requestDto.ChangeUserTypeRequest;
import com.sallejoven.backend.model.requestDto.GroupRequest;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserGroupService;
import com.sallejoven.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@PreAuthorize("isAuthenticated()")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupMapper groupMapper;
    private final UserGroupService userGroupService;
    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/")
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        List<GroupSalle> groupList = groupService.findAll();
        List<GroupDto> groupDtos = groupList.stream()
                                            .map(groupMapper::toGroupDto)
                                            .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @PreAuthorize("@authz.canManageEvent(#eventId)")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<GroupDto>> getGroupsByEvent(@PathVariable Long eventId)  {
        List<GroupSalle> groupList = groupService.findAllByEvent(eventId);
        List<GroupDto> groupDtos = groupList.stream()
                                            .map(groupMapper::toGroupDto)
                                            .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterRole(#centerId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<UserGroupDto>> getAllGroupsByCenter(@PathVariable Long centerId)  {
        List<GroupSalle> groupList = groupService.findGroupsByCenterId(centerId);
        List<UserGroupDto> groupDtos = groupList.stream()
                .map(g -> new UserGroupDto(UserType.ADMIN.toInt(), g.getId(), null, g.getStage()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long groupId)  {
        GroupSalle group = groupService.findById(groupId);
        return ResponseEntity.ok(GroupResponse.from(group));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterRole(#request.centerId(), 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PostMapping("/")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request)  {
        GroupSalle saved = groupService.createGroup(request.centerId(), request.stage());
        return ResponseEntity.ok(GroupResponse.from(saved));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#id, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroup(@PathVariable Long id, @Valid @RequestBody GroupRequest request)  {
        GroupSalle group = groupService.findById(id);
        group.setStage(request.stage());
        GroupSalle saved = groupService.saveGroup(group);
        return ResponseEntity.ok(GroupResponse.from(saved));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#id, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id)  {
        if (groupService.findById(id) != null) {
            groupService.deleteGroup(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#fromGroupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PutMapping("/user/{userId}/from/{fromGroupId}/to/{toGroupId}")
    public ResponseEntity<Void> moveUserBetweenGroups(@PathVariable Long userId,
                                                      @PathVariable Long fromGroupId,
                                                      @PathVariable Long toGroupId)  {

        UserSalle user = userService.findByUserId(userId);
        userGroupService.moveUserBetweenGroups(user, fromGroupId, toGroupId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PostMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> addUserToGroup(@PathVariable Long userId,
                                               @PathVariable Long groupId,
                                               @Valid @RequestBody ChangeUserTypeRequest request)  {
        userGroupService.addUserToGroup(userService.findByUserId(userId), groupId, request.userType());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @DeleteMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> unlinkUserFromGroupByUserAndGroup(@PathVariable Long userId,
                                                                  @PathVariable Long groupId)  {
        userGroupService.unlinkByUserAndGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PutMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> changeUserGroupRole(@PathVariable Long userId,
                                                    @PathVariable Long groupId,
                                                    @Valid @RequestBody ChangeUserTypeRequest request)  {
        userGroupService.changeRoleByUserAndGroup(userId, groupId, request.userType());
        return ResponseEntity.noContent().build();
    }

}
