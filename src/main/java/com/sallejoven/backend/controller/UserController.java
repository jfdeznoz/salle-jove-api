package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.UserDto;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequestOptional;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.PastoralDelegateImporterService;
import com.sallejoven.backend.service.UserCenterService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final SalleConverters salleConverters;
    private final GroupService groupService;
    private final UserGroupService userGroupService;
    private final PastoralDelegateImporterService pastoralDelegateImporterService;
    private final UserCenterService userCenterService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserSalle>> getAllUsers() {
        List<UserSalle> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserSalle> getUserById(@PathVariable Long id) {
        Optional<UserSalle> user = userService.findById(id);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@authz.hasCenterOfGroup(#groupId,'PASTORAL_DELEGATE','GROUP_LEADER') || @authz.hasGroupRole(#groupId,'ANIMATOR')")
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<UserDto>> getUsersByGroupId(@PathVariable Long groupId) throws SalleException {
        List<UserGroup> users = userGroupService.findByGroupId(groupId);
        List<UserDto> result = users.stream().map(user -> {
            try {
                return salleConverters.buildSelfUserInfo(user);
            } catch (SalleException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@authz.hasCenterRole(#centerId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/center/{centerId}/leaders")
    public ResponseEntity<List<UserDto>> getCenterLeadersByCenter(@PathVariable Long centerId) throws SalleException {
        var ucs = userCenterService.findActiveByCenterForCurrentYear(centerId);

        var result = ucs.stream()
                .filter(uc -> {
                    Integer t = uc.getUserType();
                    return t != null && (t == 2 || t == 3); // 2=GROUP_LEADER, 3=PASTORAL_DELEGATE
                })
                .map(uc -> salleConverters.userSalleToUserDto(uc.getUser(), uc.getUserType()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@authz.hasCenterRole(#centerId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/catechist/center/{centerId}")
    public ResponseEntity<List<UserSelfDto>> getUserByCenterId(@PathVariable Long centerId) throws SalleException {
        List<UserSalle> users = userService.getCatechistsByCenter(centerId);
        List<UserSelfDto> result = users.stream().map(user -> {
            try {
                return salleConverters.buildSelfUserInfo(user);
            } catch (SalleException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/self")
    public UserSelfDto getSelfData() throws SalleException {
        UserSalle user = authService.getCurrentUser();
        return salleConverters.buildSelfUserInfo(user);
    }

    @PreAuthorize("@authz.isAnyManagerType()")
    @GetMapping("/search")
    public ResponseEntity<List<UserSelfDto>> searchUsers(@RequestParam("search") String search) throws SalleException {
        UserSalle me = authService.getCurrentUser();

        var users = userService.searchUsersSmart(search, me);
        var result = users.stream()
                .map(u -> {
                    try { return salleConverters.buildSelfUserInfo(u); }
                    catch (SalleException e) { throw new RuntimeException(e); }
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@authz.canCreateUser(#userRequest)")
    @PostMapping
    public ResponseEntity<UserSelfDto> createUser(@RequestBody UserSalleRequest userRequest) throws SalleException {
        UserSalle savedUser = userService.saveUser(userRequest);
        UserSelfDto dto = salleConverters.buildSelfUserInfo(savedUser);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("@authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @PostMapping("/{groupId}/add-existing")
    public ResponseEntity<Void> addExistingUserToGroup( @PathVariable Long groupId, @RequestBody Map<String, Long> body) throws SalleException {
        Long userId = body.get("userId");
        Long userType = body.get("userType");

        userService.addUserToGroup(userId, groupId, userType);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.hasCenterOfGroup(#groupId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @DeleteMapping("/{id}/group/{groupId}")
    public ResponseEntity<Void> deleteUserToGroup( @PathVariable Long groupId, @PathVariable Long userId) throws SalleException {
        GroupSalle group = groupService.findById(groupId);

        UserSalle user = userService.findByUserId(userId);
        userService.removeUserFromGroup(user, group);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canManageUser(#id) || @authz.canEditUserAsAnimator(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<UserSelfDto> updateUser(@PathVariable Long id, @RequestBody UserSalleRequestOptional dto) {
        try {
            UserSalle updatedUser = userService.updateUserFromDto(id, dto);
            UserSelfDto userDto = salleConverters.buildSelfUserInfo(updatedUser);
            return ResponseEntity.ok(userDto);
        } catch (SalleException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@authz.canManageUser(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) throws SalleException {
        if (userService.findById(id).isPresent()) {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/import-users")
    public ResponseEntity<String> importUsers() throws Exception {
       /* String filePath = "C:\\Users\\jfmun\\OneDrive\\Escritorio\\usuarios.csv";
        userImporterService.importUsersFromCsv(filePath);*/
        return ResponseEntity.ok("Import done");
    }

    @PostMapping("/import-group-leaders")
    public ResponseEntity<String> importGroupLeaders() throws Exception {
        /*String filePath = "C:\\Users\\jfmun\\OneDrive\\Escritorio\\coordinadores.csv";
        groupLeaderImporterService.importGroupLeaders(filePath);*/
        return ResponseEntity.ok("Importación completada");
    }

    @PostMapping("/import-pastoral-delegates")
    public ResponseEntity<String> importPastoralDelegates() throws Exception {
        String filePath = "C:\\Users\\jfmun\\OneDrive\\Escritorio\\archivo.csv";
        pastoralDelegateImporterService.importPastoralDelegatesByCenterId(filePath);
        return ResponseEntity.ok("Importación completada");
    }

}