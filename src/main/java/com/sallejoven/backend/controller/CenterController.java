package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.CenterDto;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.dto.UserCenterDto;
import com.sallejoven.backend.model.dto.UserCenterGroupsDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserCenterService;
import com.sallejoven.backend.service.UserService;
import com.sallejoven.backend.utils.SalleConverters;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/centers")
@RequiredArgsConstructor
public class CenterController {

    private final CenterService centerService;
    private final GroupService groupService;
    private final UserService userService;
    private final AuthService authService;
    private final SalleConverters salleConverters;
    private final AcademicStateService academicStateService;
    private final UserCenterService userCenterService;

    @GetMapping
    public ResponseEntity<List<CenterDto>> listAll() {
        List<Center> centers = centerService.getAllCentersWithGroups();
        List<CenterDto> dtos = centers.stream().map(center -> {
            var groups = centerService.getGroupsForCenter(center);
            var groupDtos = groups.stream().map(g ->
                GroupDto.builder()
                        .groupId(Math.toIntExact(g.getId()))
                        .centerId(Math.toIntExact(center.getId()))
                        .stage(g.getStage())
                        .centerName(center.getName())
                        .cityName(center.getCity())
                        .build()
            ).collect(Collectors.toList());
            return CenterDto.builder()
                    .id(center.getId())
                    .city(center.getCity())
                    .name(center.getName())
                    .groups(groupDtos)
                    .build();
        }).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{centerId}/groups")
    public ResponseEntity<List<GroupDto>> listGroups(
            @PathVariable Long centerId) throws SalleException {

        Center center = centerService.getAllCentersWithGroups().stream()
                .filter(c -> c.getId().equals(centerId))
                .findFirst()
                .orElseThrow(() -> new SalleException(ErrorCodes.CENTER_NOT_FOUND));

        List<GroupSalle> groups = groupService.findByCenter(center);
        List<GroupDto> dtos = groups.stream().map(g ->
            GroupDto.builder()
                    .groupId(Math.toIntExact(g.getId()))
                    .centerId(Math.toIntExact(center.getId()))
                    .stage(g.getStage())
                    .centerName(center.getName())
                    .cityName(center.getCity())
                    .build()
        ).toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserCenterGroupsDto>> userCenters(@PathVariable Long userId) throws SalleException {
        UserSalle user = userService.findByUserId(userId);

        int visibleYear = academicStateService.getVisibleYear();

        List<UserGroup> userGroups = user.getGroups().stream()
                .filter(ug -> ug.getDeletedAt() == null
                        && ug.getGroup() != null
                        && ug.getYear() == visibleYear)
                .collect(Collectors.toList());

        List<UserCenterGroupsDto> dtos = salleConverters.userGroupsToUserCenters(userGroups);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/me")
    public ResponseEntity<List<UserCenterGroupsDto>> myCenters(Principal principal) throws SalleException {
        UserSalle me = userService.findByEmail(principal.getName());
        List<Role> roles = authService.getCurrentUserRoles();

        var raws = centerService.getCentersForUserRaw(me, roles);

        List<UserCenterGroupsDto> dtos = raws.stream()
                .map(r -> salleConverters.toUserCenterGroupsDto(
                        r.getCenter(),
                        r.getGroups(),
                        r.getUserType()
                ))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/user/{userId}/center")
    public ResponseEntity<UserCenterDto> getUserCenter(@PathVariable Long userId) throws SalleException {
        UserCenter uc = userCenterService.findByUserForCurrentYear(userId);

        if (uc == null) {
            return ResponseEntity.noContent().build();
        }

        UserCenterDto dto = salleConverters.userCenterToDto(uc);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/user/{userId}/center/{centerId}")
    public ResponseEntity<UserCenterDto> addCenterRole(
            @PathVariable Long userId,
            @PathVariable Long centerId,
            @RequestBody Map<String, Integer> body
    ) throws SalleException {
        Integer userType = body.get("userType");
        UserCenter uc = userCenterService.addCenterRole(userId, centerId, userType);
        return ResponseEntity.ok(salleConverters.userCenterToDto(uc));
    }

    @PutMapping("/user-center/{userCenterId}")
    public ResponseEntity<UserCenterDto> changeCenterRole(
            @PathVariable Long userCenterId,
            @RequestBody Map<String, Integer> body
    ) throws SalleException {
        Integer userType = body.get("userType");
        UserCenter uc = userCenterService.updateCenterRole(userCenterId, userType);
        return ResponseEntity.ok(salleConverters.userCenterToDto(uc));
    }

    @DeleteMapping("/user-center/{userCenterId}")
    public ResponseEntity<Void> deleteCenterRole(@PathVariable Long userCenterId) throws SalleException {
        userCenterService.softDelete(userCenterId);
        return ResponseEntity.noContent().build();
    }

}