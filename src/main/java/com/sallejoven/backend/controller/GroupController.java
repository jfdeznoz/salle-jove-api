package com.sallejoven.backend.controller;

import com.sallejoven.backend.mapper.GroupMapper;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.dto.GroupResponse;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.enums.UserType;
import com.sallejoven.backend.model.requestDto.ChangeUserGroupTypeRequest;
import com.sallejoven.backend.model.requestDto.GroupRequest;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.EventService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserGroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

@PreAuthorize("isAuthenticated()")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupMapper groupMapper;
    private final UserGroupService userGroupService;
    private final EventService eventService;
    private final CenterService centerService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/")
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        return ResponseEntity.ok(groupService.findAll().stream().map(groupMapper::toGroupDto).collect(Collectors.toList()));
    }

    @PreAuthorize("@authz.canManageEvent(#eventId)")
    @GetMapping("/event/{eventId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<GroupDto>> getGroupsByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(groupService.findAllByEvent(eventId).stream().map(groupMapper::toGroupDto).collect(Collectors.toList()));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterRole(#centerId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/center/{centerId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<UserGroupDto>> getAllGroupsByCenter(@PathVariable UUID centerId) {
        List<UserGroupDto> groupDtos = groupService.findGroupsByCenterId(centerId).stream()
                .map(group -> new UserGroupDto(UserType.ADMIN.toInt(), group.getUuid(), null, group.getStage()))
                .toList();
        return ResponseEntity.ok(groupDtos);
    }

    @PreAuthorize("@authz.isAnyManagerType() || @authz.hasGroupRole(#groupId, 'ANIMATOR')")
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable UUID groupId) {
        return ResponseEntity.ok(GroupResponse.from(groupService.findById(groupId)));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterRole(@authz.requestCenterId(#request), 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PostMapping("/")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request) {
        GroupSalle saved = groupService.createGroup(UUID.fromString(request.centerUuid()), request.stage());
        return ResponseEntity.ok(GroupResponse.from(saved));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#id, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroup(@PathVariable UUID id, @Valid @RequestBody GroupRequest request) {
        GroupSalle group = groupService.findById(id);
        group.setStage(request.stage());
        return ResponseEntity.ok(GroupResponse.from(groupService.saveGroup(group)));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#id, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#fromGroupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PutMapping("/user/{userId}/from/{fromGroupId}/to/{toGroupId}")
    public ResponseEntity<Void> moveUserBetweenGroups(@PathVariable UUID userId, @PathVariable UUID fromGroupId, @PathVariable UUID toGroupId) {
        userGroupService.moveUserBetweenGroups(userId, fromGroupId, toGroupId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PostMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> addUserToGroup(
            @PathVariable UUID userId,
            @PathVariable UUID groupId,
            @Valid @RequestBody ChangeUserGroupTypeRequest request
    ) {
        userGroupService.addUserToGroup(userId, groupId, request.userType());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @DeleteMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> unlinkUserFromGroupByUserAndGroup(@PathVariable UUID userId, @PathVariable UUID groupId) {
        userGroupService.unlinkByUserAndGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PutMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> changeUserGroupRole(
            @PathVariable UUID userId,
            @PathVariable UUID groupId,
            @Valid @RequestBody ChangeUserGroupTypeRequest request
    ) {
        userGroupService.changeRoleByUserAndGroup(userId, groupId, request.userType());
        return ResponseEntity.noContent().build();
    }
}
