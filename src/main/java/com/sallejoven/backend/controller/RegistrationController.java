package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.UserPendingDto;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.service.RegistrationService;
import com.sallejoven.backend.utils.SalleConverters;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final SalleConverters converters;

    @PostMapping("/public/register")
    public ResponseEntity<Void> registerPublic(@RequestBody UserSalleRequest req) throws SalleException {
        registrationService.registerPublic(req);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/api/registration/pending")
    public ResponseEntity<List<UserPendingDto>> listPending() {
        List<UserPendingDto> pendings = registrationService.listPending();
        return ResponseEntity.ok(pendings);
    }

    @PutMapping("/api/registration/pending/{id}/approve")
    public ResponseEntity<UserSelfDto> approve(@PathVariable Long id) throws SalleException {
        UserSalle created = registrationService.approvePending(id);
        return ResponseEntity.ok(converters.buildSelfUserInfo(created));
    }

    @DeleteMapping("/api/registration/pending/{id}")
    public ResponseEntity<Void> reject(@PathVariable Long id) throws SalleException {
        registrationService.rejectPending(id);
        return ResponseEntity.noContent().build();
    }
}