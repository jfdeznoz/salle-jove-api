package com.sallejoven.backend.controller;

import com.sallejoven.backend.model.dto.UserPendingDto;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.service.RegistrationService;
import com.sallejoven.backend.service.assembler.UserAssembler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final UserAssembler userAssembler;

    @PostMapping("/public/register")
    public ResponseEntity<Void> registerPublic(@Valid @RequestBody UserSalleRequest req) {
        registrationService.registerPublic(req);
        return ResponseEntity.accepted().build();
    }

    @PreAuthorize("@authz.isAnyManagerType()")
    @GetMapping("/api/registration/pending")
    public ResponseEntity<List<UserPendingDto>> listPending() {
        List<UserPending> pendings = registrationService.listPending();
        List<UserPendingDto> dtos = pendings.stream()
                .map(userAssembler::toPendingDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("@authz.canModeratePending(#id)")
    @PutMapping("/api/registration/pending/{id}/approve")
    public ResponseEntity<UserSelfDto> approve(@PathVariable Long id) {
        UserSalle created = registrationService.approvePending(id);
        return ResponseEntity.ok(userAssembler.toSelfDto(created));
    }

    @PreAuthorize("@authz.canModeratePending(#id)")
    @DeleteMapping("/api/registration/pending/{id}")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        registrationService.rejectPending(id);
        return ResponseEntity.noContent().build();
    }
}
