package com.sallejoven.backend.controller;

import com.sallejoven.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/auth/password-reset")
@Validated
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    public record StartReq(@NotBlank @Email String email) {}
    public record ConfirmReq(@NotBlank String token,
                             @NotBlank @Size(min = 4, max = 200) String newPassword) {}

    @PostMapping("/start")
    public ResponseEntity<Void> start(@RequestBody @Valid StartReq req) {
        service.startReset(req.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestBody @Valid ConfirmReq req) {
        service.confirmReset(req.token(), req.newPassword());
        return ResponseEntity.ok().build();
    }
}