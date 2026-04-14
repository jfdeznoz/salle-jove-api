package com.sallejoven.backend.controller;

import com.sallejoven.backend.mapper.CenterMapper;
import com.sallejoven.backend.model.dto.CenterDto;
import com.sallejoven.backend.model.dto.UserCenterDto;
import com.sallejoven.backend.model.dto.UserCenterGroupsDto;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.requestDto.CenterRequest;
import com.sallejoven.backend.model.requestDto.ChangeUserTypeRequest;
import com.sallejoven.backend.model.requestDto.ForkCenterRequest;
import com.sallejoven.backend.model.requestDto.MergeCenterRequest;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.UserCenterService;
import com.sallejoven.backend.service.UserService;
import com.sallejoven.backend.service.assembler.UserAssembler;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CenterDto> createCenter(@Valid @RequestBody CenterRequest request) {
        return ResponseEntity.ok(centerService.createCenter(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CenterDto> updateCenter(@PathVariable UUID id, @Valid @RequestBody CenterRequest request) {
        return ResponseEntity.ok(centerService.updateCenter(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCenter(@PathVariable UUID id) {
        centerService.deleteCenter(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/fork")
    public ResponseEntity<CenterDto> forkCenter(@PathVariable UUID id, @Valid @RequestBody ForkCenterRequest request) {
        return ResponseEntity.ok(centerService.forkCenter(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{targetId}/merge")
    public ResponseEntity<CenterDto> mergeCenter(@PathVariable UUID targetId, @Valid @RequestBody MergeCenterRequest request) {
        UUID sourceUuid = request.sourceCenterUuid() != null ? UUID.fromString(request.sourceCenterUuid()) : null;
        return ResponseEntity.ok(centerService.mergeCenter(targetId, sourceUuid));
    }

    @PreAuthorize("@authz.isAnyManagerType()")
    @GetMapping("/me-list")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<CenterDto>> listMyCenters() {
        UserSalle me = authService.getCurrentUser();
        List<Role> roles = authService.getCurrentUserRoles();
        return ResponseEntity.ok(centerService.listMyCenters(me, roles));
    }

    @PreAuthorize("@authz.canViewUserGroups(#userId)")
    @GetMapping("/user/{userId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<UserCenterGroupsDto>> userCenters(@PathVariable UUID userId) {
        UserSalle user = userService.findByUserId(userId);
        List<UserGroup> userGroups = centerService.getActiveUserGroupsForYear(user);
        return ResponseEntity.ok(userAssembler.toUserCenterGroupsDtos(userGroups));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<UserCenterGroupsDto>> myCenters() {
        UserSalle me = authService.getCurrentUser();
        List<Role> roles = authService.getCurrentUserRoles();
        var raws = centerService.getCentersForUserRaw(me, roles);
        List<UserCenterGroupsDto> dtos = raws.stream()
                .map(raw -> new UserCenterGroupsDto(raw.center().getUuid(), raw.center().getName(), raw.center().getCity(), raw.groups()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("@authz.canViewUserCenters(#userId)")
    @GetMapping("/user/{userId}/center")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<UserCenterDto>> getUserCenters(@PathVariable UUID userId) {
        List<UserCenter> list = userCenterService.findByUserForCurrentYear(userId);
        if (list == null || list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list.stream().map(centerMapper::toUserCenterDto).toList());
    }

    @PreAuthorize("@authz.canManageCenterAsDelegate(#centerId)")
    @PostMapping("/user/{userId}/center/{centerId}")
    public ResponseEntity<UserCenterDto> addCenterRole(
            @PathVariable UUID userId,
            @PathVariable UUID centerId,
            @Valid @RequestBody ChangeUserTypeRequest request
    ) {
        UserCenter userCenter = userCenterService.addCenterRole(userId, centerId, request.userType());
        return ResponseEntity.ok(centerMapper.toUserCenterDto(userCenter));
    }

    @PreAuthorize("@authz.canManageUserCenterAsDelegate(#userCenterId)")
    @PutMapping("/user-center/{userCenterId}")
    public ResponseEntity<UserCenterDto> changeCenterRole(
            @PathVariable UUID userCenterId,
            @Valid @RequestBody ChangeUserTypeRequest request
    ) {
        UserCenter userCenter = userCenterService.updateCenterRole(userCenterId, request.userType());
        return ResponseEntity.ok(centerMapper.toUserCenterDto(userCenter));
    }

    @PreAuthorize("@authz.canManageUserCenterAsDelegate(#userCenterId)")
    @DeleteMapping("/user-center/{userCenterId}")
    public ResponseEntity<Void> deleteCenterRole(@PathVariable UUID userCenterId) {
        userCenterService.softDelete(userCenterId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canManageCenterAsDelegate(#centerId)")
    @DeleteMapping("/user/{userId}/center/{centerId}")
    public ResponseEntity<Void> deleteCenterRole(@PathVariable UUID userId, @PathVariable UUID centerId) {
        UserCenter userCenter = userCenterService.findByUserAndCenter(userId, centerId);
        userCenterService.softDelete(userCenter.getUuid());
        return ResponseEntity.noContent().build();
    }
}
