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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final SalleConverters salleConverters;
    private final UserGroupService userGroupService;
    private final UserService userService;

   @GetMapping("/")
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        List<GroupSalle> groupList = groupService.findAll();
        List<GroupDto> groupDtos = groupList.stream()
                                            .map(salleConverters::groupToDto)
                                            .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<GroupDto>> getAllGroupsByEvent(@PathVariable Long eventId) throws SalleException {
        List<GroupSalle> groupList = groupService.findAllByEvent(eventId);
        List<GroupDto> groupDtos = groupList.stream()
                                            .map(salleConverters::groupToDto)
                                            .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<UserGroupDto>> getAllGroupsByCenter(@PathVariable Long centerId) throws SalleException {
        List<GroupSalle> groupList = groupService.findGroupsByCenterId(centerId);
        List<UserGroupDto> groupDtos = groupList.stream()
                .map(salleConverters::groupToUserGroupDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupSalle> getGroupById(@PathVariable Long id) {
        Optional<GroupSalle> group = groupService.findById(id);
        return group.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    public ResponseEntity<GroupSalle> createGroup(@RequestBody GroupSalle group) {
        return ResponseEntity.ok(groupService.saveGroup(group));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupSalle> updateGroup(@PathVariable Long id, @RequestBody GroupSalle groupDetails) {
        Optional<GroupSalle> group = groupService.findById(id);
        if (group.isPresent()) {
            GroupSalle existingGroup = group.get();
            existingGroup.setStage(groupDetails.getStage());
            return ResponseEntity.ok(groupService.saveGroup(existingGroup));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        if (groupService.findById(id).isPresent()) {
            groupService.deleteGroup(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/user/{userId}/from/{fromGroupId}/to/{toGroupId}")
    public ResponseEntity<Void> moveUserBetweenGroups(@PathVariable Long userId,
                                                      @PathVariable Long fromGroupId,
                                                      @PathVariable Long toGroupId) throws SalleException {

        UserSalle user = userService.findByUserId(userId);
        userGroupService.moveUserBetweenGroups(user, fromGroupId, toGroupId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> addUserToGroup(@PathVariable Long userId,
                                               @PathVariable Long groupId,
                                               @RequestBody Map<String,Integer> body) throws SalleException {
        int userType = body.get("userType");
        userGroupService.addUserToGroup(userService.findByUserId(userId), groupId, userType);
        return ResponseEntity.noContent().build(); // o created(URI)
    }

    @DeleteMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> unlinkUserFromGroupByUserAndGroup(@PathVariable Long userId,
                                                                  @PathVariable Long groupId) throws SalleException {
        userGroupService.unlinkByUserAndGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/user/{userId}/group/{groupId}")
    public ResponseEntity<Void> changeUserGroupRole(@PathVariable Long userId,
                                                    @PathVariable Long groupId,
                                                    @RequestBody Map<String,Integer> body) throws SalleException {
        int newRole = body.get("userType");
        userGroupService.changeRoleByUserAndGroup(userId, groupId, newRole);
        return ResponseEntity.noContent().build();
    }

}