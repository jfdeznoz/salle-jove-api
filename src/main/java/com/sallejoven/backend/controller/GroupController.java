package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserGroupService;
import com.sallejoven.backend.service.UserService;
import com.sallejoven.backend.utils.SalleConverters;

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
import java.util.Map;
import java.util.stream.Collectors;

@PreAuthorize("isAuthenticated()")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final SalleConverters salleConverters;
    private final UserGroupService userGroupService;
    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/")
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        List<GroupSalle> groupList = groupService.findAll();
        List<GroupDto> groupDtos = groupList.stream()
                                            .map(salleConverters::groupToDto)
                                            .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @PreAuthorize("@authz.canManageEvent(#eventId)")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<GroupDto>> getGroupsByEvent(@PathVariable Long eventId) throws SalleException {
        List<GroupSalle> groupList = groupService.findAllByEvent(eventId);
        List<GroupDto> groupDtos = groupList.stream()
                                            .map(salleConverters::groupToDto)
                                            .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterRole(#centerId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<UserGroupDto>> getAllGroupsByCenter(@PathVariable Long centerId) throws SalleException {
        List<GroupSalle> groupList = groupService.findGroupsByCenterId(centerId);
        List<UserGroupDto> groupDtos = groupList.stream()
                .map(salleConverters::groupToUserGroupDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupSalle> getGroupById(@PathVariable Long groupId) throws SalleException {
        GroupSalle group = groupService.findById(groupId);
        return ResponseEntity.ok(group);
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterRole(#group.center.id, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PostMapping("/")
    public ResponseEntity<GroupSalle> createGroup(@RequestBody GroupSalle group) {
        return ResponseEntity.ok(groupService.saveGroup(group));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterRole(#groupDetails.center.id, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PutMapping("/{id}")
    public ResponseEntity<GroupSalle> updateGroup(@PathVariable Long id, @RequestBody GroupSalle groupDetails) throws SalleException {
        GroupSalle group = groupService.findById(id);
        if (group != null) {
            group.setStage(groupDetails.getStage());
            return ResponseEntity.ok(groupService.saveGroup(group));
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#id, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) throws SalleException {
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
                                                      @PathVariable Long toGroupId) throws SalleException {

        UserSalle user = userService.findByUserId(userId);
        userGroupService.moveUserBetweenGroups(user, fromGroupId, toGroupId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PostMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> addUserToGroup(@PathVariable Long userId,
                                               @PathVariable Long groupId,
                                               @RequestBody Map<String,Integer> body) throws SalleException {
        int userType = body.get("userType");
        userGroupService.addUserToGroup(userService.findByUserId(userId), groupId, userType);
        return ResponseEntity.noContent().build(); // o created(URI)
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @DeleteMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> unlinkUserFromGroupByUserAndGroup(@PathVariable Long userId,
                                                                  @PathVariable Long groupId) throws SalleException {
        userGroupService.unlinkByUserAndGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PutMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> changeUserGroupRole(@PathVariable Long userId,
                                                    @PathVariable Long groupId,
                                                    @RequestBody Map<String,Integer> body) throws SalleException {
        int newRole = body.get("userType");
        userGroupService.changeRoleByUserAndGroup(userId, groupId, newRole);
        return ResponseEntity.noContent().build();
    }

}