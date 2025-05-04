package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.GroupService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
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
    public List<UserSelfDto> getUserByGroupId(@PathVariable Long groupId) {
        List<UserSalle> users = userService.getUsersByGroupId(groupId);
        return users.stream().map(salleConverters::userToDto).collect(Collectors.toList());
    }

    @GetMapping("/self")
    public UserSelfDto getSelfData() throws SalleException {
        String userEmail = authService.getCurrentUserEmail();
        return salleConverters.buildSelfUserInfo(userEmail);
    }

    @PostMapping("/")
    public ResponseEntity<UserSalle> createUser(@RequestBody UserSalle user) {
        return ResponseEntity.ok(userService.saveUser(user));
    }

    @PostMapping("/group/{id}")
    public ResponseEntity<UserSalle> createUser(@PathVariable Long id, @RequestBody UserSalleRequest user) {
        Optional<GroupSalle> group = groupService.findById(id);
        Set<GroupSalle> userGroups = new HashSet<>();
        if (group.isPresent()) {
            userGroups.add(group.get());
        }

        return ResponseEntity.ok(userService.saveUser(user, userGroups));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserSalle> updateUser(@PathVariable Long id, @RequestBody UserSalleRequest userDetails) {
        try {
            UserSalle updatedUser = userService.updateUserFields(id, userDetails);
            return ResponseEntity.ok(updatedUser);
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

}