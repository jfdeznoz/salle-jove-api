package com.sallejoven.backend.controller;

import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

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

    @PostMapping
    public ResponseEntity<UserSalle> createUser(@RequestBody UserSalle user) {
        return ResponseEntity.ok(userService.saveUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserSalle> updateUser(@PathVariable Long id, @RequestBody UserSalle userDetails) {
        Optional<UserSalle> user = userService.findById(id);
        if (user.isPresent()) {
            UserSalle existingUser = user.get();
            existingUser.setName(userDetails.getName());
            existingUser.setLastName(userDetails.getLastName());
            existingUser.setEmail(userDetails.getEmail());
            existingUser.setDni(userDetails.getDni());
            existingUser.setPhone(userDetails.getPhone());
            //existingUser.setRole(userDetails.getRoles());
            return ResponseEntity.ok(userService.saveUser(existingUser));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.findById(id).isPresent()) {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}