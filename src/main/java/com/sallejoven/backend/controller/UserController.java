package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.mapper.UserMapper;
import com.sallejoven.backend.model.dto.UserDto;
import com.sallejoven.backend.model.dto.UserResponse;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.AddUserToGroupRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequestOptional;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserCenterService;
import com.sallejoven.backend.service.UserGroupService;
import com.sallejoven.backend.service.UserService;
import com.sallejoven.backend.service.assembler.UserAssembler;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final UserAssembler userAssembler;
    private final UserMapper userMapper;
    private final GroupService groupService;
    private final UserGroupService userGroupService;
    private final UserCenterService userCenterService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.findAllUsers().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@authz.hasCenterOfGroup(#groupId,'PASTORAL_DELEGATE','GROUP_LEADER') || @authz.hasGroupRole(#groupId,'ANIMATOR')")
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<UserDto>> getUsersByGroupId(@PathVariable Long groupId) {
        List<UserGroup> users = userGroupService.findByGroupId(groupId);
        List<UserDto> result = users.stream()
                .map(userMapper::toUserDtoFromUserGroup)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@authz.hasCenterRole(#centerId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/center/{centerId}/leaders")
    public ResponseEntity<List<UserDto>> getCenterLeadersByCenter(@PathVariable Long centerId) {
        var ucs = userCenterService.findActiveByCenterForCurrentYear(centerId);

        var result = ucs.stream()
                .filter(uc -> {
                    Integer t = uc.getUserType();
                    return t != null && (t == 2 || t == 3); // 2=GROUP_LEADER, 3=PASTORAL_DELEGATE
                })
                .map(uc -> userMapper.toUserDto(uc.getUser(), uc.getUserType()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@authz.hasCenterRole(#centerId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/catechist/center/{centerId}")
    public ResponseEntity<List<UserSelfDto>> getUserByCenterId(@PathVariable Long centerId) {
        List<UserSalle> users = userService.getCatechistsByCenter(centerId);
        List<UserSelfDto> result = users.stream()
                .map(userAssembler::toSelfDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/self")
    public UserSelfDto getSelfData() {
        UserSalle user = authService.getCurrentUser();
        return userAssembler.toSelfDto(user);
    }

    @PreAuthorize("@authz.isAnyManagerType()")
    @GetMapping("/search")
    public ResponseEntity<List<UserSelfDto>> searchUsers(@RequestParam("search") String search) {
        UserSalle me = authService.getCurrentUser();

        var users = userService.searchUsersSmart(search, me);
        var result = users.stream()
                .map(userAssembler::toSelfDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@authz.canCreateUser(#userRequest)")
    @PostMapping
    public ResponseEntity<UserSelfDto> createUser(@Valid @RequestBody UserSalleRequest userRequest) {
        UserSalle savedUser = userService.saveUser(userRequest);
        UserSelfDto dto = userAssembler.toSelfDto(savedUser);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("@authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PostMapping("/{groupId}/add-existing")
    public ResponseEntity<Void> addExistingUserToGroup(@PathVariable Long groupId,
                                                       @Valid @RequestBody AddUserToGroupRequest request) {
        userService.addUserToGroup(request.userId(), groupId, request.userType().longValue());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @DeleteMapping("/{id}/group/{groupId}")
    public ResponseEntity<Void> deleteUserToGroup(@PathVariable Long groupId, @PathVariable("id") Long userId) {
        GroupSalle group = groupService.findById(groupId);

        UserSalle user = userService.findByUserId(userId);
        userService.removeUserFromGroup(user, group);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canManageUser(#id) || @authz.canEditUserAsAnimator(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<UserSelfDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserSalleRequestOptional dto) {
        try {
            UserSalle updatedUser = userService.updateUserFromDto(id, dto);
            UserSelfDto userDto = userAssembler.toSelfDto(updatedUser);
            return ResponseEntity.ok(userDto);
        } catch (SalleException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@authz.canManageUser(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.findById(id).isPresent()) {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
