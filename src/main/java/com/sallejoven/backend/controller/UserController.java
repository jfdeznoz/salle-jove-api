package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.requestDto.UserSalleRequestOptional;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.GroupLeaderImporterService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.PastoralDelegateImporterService;
import com.sallejoven.backend.service.UserImporterService;
import com.sallejoven.backend.service.UserService;
import com.sallejoven.backend.utils.SalleConverters;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final SalleConverters salleConverters;
    private final GroupService groupService;
    private final UserImporterService userImporterService;
    private final PastoralDelegateImporterService pastoralDelegateImporterService;
    private final GroupLeaderImporterService groupLeaderImporterService;

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

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<UserSelfDto>> getUserByGroupId(@PathVariable Long groupId) {
        List<UserSalle> users = userService.getUsersByGroupId(groupId);
        List<UserSelfDto> result = users.stream().map(user -> {
            try {
                return salleConverters.buildSelfUserInfo(user);
            } catch (SalleException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/catechist/center/{centerId}")
    public ResponseEntity<List<UserSelfDto>> getUserByCenterId(@PathVariable Long centerId, @RequestParam(required = false) String role) {
        List<UserSalle> users = userService.getUsersByCenterId(centerId, role);
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
        String userEmail = authService.getCurrentUserEmail();
        return salleConverters.buildSelfUserInfo(userEmail);
    }

    @PostMapping("/")
    public ResponseEntity<UserSalle> createUser(@RequestBody UserSalleRequest userRequest) {
        return ResponseEntity.ok(userService.saveUser(userRequest));
    }

    @PostMapping("/{groupId}")
    public ResponseEntity<UserSalle> createUser(@PathVariable Long groupId, @RequestBody UserSalleRequest userRequest) throws SalleException {
        GroupSalle group = groupService.findById(groupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        Set<GroupSalle> userGroups = Set.of(group);
        UserSalle savedUser = userService.saveUser(userRequest, userGroups);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/{groupId}/add-existing")
    public ResponseEntity<Void> addExistingUserToGroup( @PathVariable Long groupId, @RequestBody Map<String, Long> body) throws SalleException {
        Long userId = body.get("userId");
        GroupSalle group = groupService.findById(groupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        UserSalle user = userService.findByUserId(userId);
        userService.addUserToGroup(user, group);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/group/{groupId}")
    public ResponseEntity<Void> deleteUserToGroup( @PathVariable Long groupId, @PathVariable Long userId) throws SalleException {
        GroupSalle group = groupService.findById(groupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        UserSalle user = userService.findByUserId(userId);
        userService.removeUserFromGroup(user, group);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/group/{fromGroupId}/to/{toGroupId}")
    public ResponseEntity<Void> moveUserBetweenGroups(@PathVariable Long userId,
                                                        @PathVariable Long fromGroupId,
                                                        @PathVariable Long toGroupId) throws SalleException {

        UserSalle user = userService.findByUserId(userId);

        GroupSalle from = groupService.findById(fromGroupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
        GroupSalle to = groupService.findById(toGroupId)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));

        userService.moveUserBetweenGroups(user, from, to);

        return ResponseEntity.noContent().build();
    }

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
        String filePath = "C:\\Users\\jfmun\\OneDrive\\Escritorio\\usuarios.csv";
        userImporterService.importUsersFromCsv(filePath);
        return ResponseEntity.ok("Import done");
    }

    @PostMapping("/import-group-leaders")
    public ResponseEntity<String> importGroupLeaders() throws Exception {
        String filePath = "C:\\Users\\jfmun\\OneDrive\\Escritorio\\coordinadores.csv";
        groupLeaderImporterService.importGroupLeaders(filePath);
        return ResponseEntity.ok("Importación completada");
    }

    @PostMapping("/import-pastoral-delegates")
    public ResponseEntity<String> importPastoralDelegates() throws Exception {
        String filePath = "C:\\Users\\jfmun\\OneDrive\\Escritorio\\archivo.csv";
        pastoralDelegateImporterService.importPastoralDelegatesByCenterId(filePath);
        return ResponseEntity.ok("Importación completada");
    }

}