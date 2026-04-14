package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.mapper.UserMapper;
import com.sallejoven.backend.model.dto.UserDto;
import com.sallejoven.backend.model.dto.UserResponse;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.AddUserToGroupRequest;
import com.sallejoven.backend.model.requestDto.ReactivateUserRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequestOptional;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserCenterService;
import com.sallejoven.backend.service.UserGroupService;
import com.sallejoven.backend.service.UserService;
import com.sallejoven.backend.service.assembler.UserAssembler;
import com.sallejoven.backend.utils.ReferenceParser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import java.util.UUID;
import java.util.stream.Collectors;

@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final UserAssembler userAssembler;
    private final UserMapper userMapper;
    private final GroupService groupService;
    private final CenterService centerService;
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

    @PreAuthorize("@authz.canManageUser(#userUuid) || @authz.isAnyManagerType()")
    @GetMapping("/{userUuid}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userUuid) {
        return userService.findById(userUuid)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@authz.hasCenterOfGroup(#groupUuid,'PASTORAL_DELEGATE','GROUP_LEADER') || @authz.hasGroupRole(#groupUuid,'ANIMATOR')")
    @GetMapping("/group/{groupUuid}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<UserDto>> getUsersByGroupId(@PathVariable UUID groupUuid) {
        List<UserGroup> users = userGroupService.findByGroupId(groupUuid);
        List<UserDto> result = users.stream()
                .map(userMapper::toUserDtoFromUserGroup)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@authz.hasCenterRole(#centerUuid, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/center/{centerUuid}/leaders")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<UserDto>> getCenterLeadersByCenter(@PathVariable UUID centerUuid) {
        var ucs = userCenterService.findActiveByCenterForCurrentYear(centerUuid);

        var result = ucs.stream()
                .filter(uc -> {
                    Integer t = uc.getUserType();
                    return t != null && (t == 2 || t == 3);
                })
                .map(uc -> userMapper.toUserDto(uc.getUser(), uc.getUserType()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@authz.hasCenterRole(#centerUuid, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/catechist/center/{centerUuid}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<UserSelfDto>> getUserByCenterId(@PathVariable UUID centerUuid) {
        List<UserSalle> users = userService.getCatechistsByCenter(centerUuid);
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<UserSelfDto>> searchUsers(@RequestParam("search") @Size(max = 100) String search) {
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

    @PreAuthorize("@authz.hasCenterOfGroup(#groupUuid, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PostMapping("/{groupUuid}/add-existing")
    public ResponseEntity<Void> addExistingUserToGroup(@PathVariable UUID groupUuid,
                                                       @Valid @RequestBody AddUserToGroupRequest request) {
        UUID userUuid = ReferenceParser.asUuid(request.userUuid())
                .orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
        userService.addUserToGroup(userUuid, groupUuid, request.userType());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.hasCenterOfGroup(#groupUuid, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @DeleteMapping("/{userUuid}/group/{groupUuid}")
    public ResponseEntity<Void> deleteUserToGroup(@PathVariable UUID userUuid, @PathVariable UUID groupUuid) {
        GroupSalle group = groupService.findById(groupUuid);
        UserSalle user = userService.findByUserId(userUuid);
        userService.removeUserFromGroup(user, group);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canManageUser(#userUuid) || @authz.canEditUserAsAnimator(#userUuid)")
    @PutMapping("/{userUuid}")
    public ResponseEntity<UserSelfDto> updateUser(@PathVariable UUID userUuid, @Valid @RequestBody UserSalleRequestOptional dto) {
        try {
            UserSalle updatedUser = userService.updateUserFromDto(userUuid, dto);
            UserSelfDto userDto = userAssembler.toSelfDto(updatedUser);
            return ResponseEntity.ok(userDto);
        } catch (SalleException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@authz.canManageUser(#userUuid)")
    @DeleteMapping("/{userUuid}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userUuid) {
        if (userService.findById(userUuid).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(userUuid);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/deleted")
    public ResponseEntity<List<UserResponse>> getDeletedUsers(
            @RequestParam(name = "search", required = false) @Size(max = 100) String search) {
        List<UserResponse> users = userService.findDeletedUsers(search).stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userUuid}/reactivate")
    public ResponseEntity<UserResponse> reactivateUser(
            @PathVariable UUID userUuid,
            @Valid @RequestBody(required = false) ReactivateUserRequest request) {
        UUID mergeFromUuid = request != null ? request.mergeFromUuid() : null;
        UserSalle reactivated = userService.reactivate(userUuid, mergeFromUuid);
        return ResponseEntity.ok(UserResponse.from(reactivated));
    }
}
