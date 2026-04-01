package com.sallejoven.backend.controller;

import com.sallejoven.backend.mapper.CenterMapper;
import com.sallejoven.backend.model.dto.CenterDto;
import com.sallejoven.backend.model.dto.UserCenterDto;
import com.sallejoven.backend.model.dto.UserCenterGroupsDto;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.model.requestDto.ChangeUserTypeRequest;
import com.sallejoven.backend.service.UserCenterService;
import com.sallejoven.backend.service.UserService;
import com.sallejoven.backend.service.assembler.UserAssembler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/centers")
@RequiredArgsConstructor
public class CenterController {

    private final CenterService centerService;
    private final UserService userService;
    private final AuthService authService;
    private final CenterMapper centerMapper;
    private final UserAssembler userAssembler;
    private final UserCenterService userCenterService;

    @PreAuthorize("@authz.isAnyManagerType()")
    @GetMapping("/me-list")
    public ResponseEntity<List<CenterDto>> listMyCenters() {
        UserSalle me = authService.getCurrentUser();
        List<Role> roles = authService.getCurrentUserRoles();
        return ResponseEntity.ok(centerService.listMyCenters(me, roles));
    }

    @PreAuthorize("@authz.canViewUserGroups(#userId)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserCenterGroupsDto>> userCenters(@PathVariable Long userId) {
        UserSalle user = userService.findByUserId(userId);
        List<UserGroup> userGroups = centerService.getActiveUserGroupsForYear(user);
        List<UserCenterGroupsDto> dtos = userAssembler.toUserCenterGroupsDtos(userGroups);
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<UserCenterGroupsDto>> myCenters() {
        UserSalle me = authService.getCurrentUser();
        List<Role> roles = authService.getCurrentUserRoles();

        var raws = centerService.getCentersForUserRaw(me, roles);

        List<UserCenterGroupsDto> dtos = raws.stream()
                .map(r -> new UserCenterGroupsDto(r.center().getId(), r.center().getName(), r.center().getCity(), r.groups()))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("@authz.canViewUserCenters(#userId)")
    @GetMapping("/user/{userId}/center")
    public ResponseEntity<List<UserCenterDto>> getUserCenters(@PathVariable Long userId) {
        List<UserCenter> list = userCenterService.findByUserForCurrentYear(userId);

        if (list == null || list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<UserCenterDto> dtos = list.stream()
                .map(centerMapper::toUserCenterDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("@authz.canManageCenterAsDelegate(#centerId)")
    @PostMapping("/user/{userId}/center/{centerId}")
    public ResponseEntity<UserCenterDto> addCenterRole(
            @PathVariable Long userId,
            @PathVariable Long centerId,
            @Valid @RequestBody ChangeUserTypeRequest request
    ) {
        UserCenter uc = userCenterService.addCenterRole(userId, centerId, request.userType());
        return ResponseEntity.ok(centerMapper.toUserCenterDto(uc));
    }

    @PreAuthorize("@authz.canManageUserCenterAsDelegate(#userCenterId)")
    @PutMapping("/user-center/{userCenterId}")
    public ResponseEntity<UserCenterDto> changeCenterRole(
            @PathVariable Long userCenterId,
            @Valid @RequestBody ChangeUserTypeRequest request
    ) {
        UserCenter uc = userCenterService.updateCenterRole(userCenterId, request.userType());
        return ResponseEntity.ok(centerMapper.toUserCenterDto(uc));
    }

    @PreAuthorize("@authz.canManageUserCenterAsDelegate(#userCenterId)")
    @DeleteMapping("/user-center/{userCenterId}")
    public ResponseEntity<Void> deleteCenterRole(@PathVariable Long userCenterId) {
        userCenterService.softDelete(userCenterId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canManageCenterAsDelegate(#centerId)")
    @DeleteMapping("/user/{userId}/center/{centerId}")
    public ResponseEntity<UserCenterDto> deleteCenterRole(
            @PathVariable Long userId,
            @PathVariable Long centerId
    ) {
        UserCenter uc = userCenterService.findByUserAndCenter(userId, centerId);
        userCenterService.softDelete(uc.getId());
        return ResponseEntity.noContent().build();
    }

}