// src/main/java/.../controller/GlobalController.java
package com.sallejoven.backend.controller;

import com.sallejoven.backend.config.security.AuthzBean;
import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.GlobalStateDto;
import com.sallejoven.backend.model.dto.UserPendingDto;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.requestDto.GlobalLockRequest;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.RegistrationService;
import com.sallejoven.backend.utils.SalleConverters;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/global")
@RequiredArgsConstructor
public class GlobalController {

    private final AcademicStateService academicStateService;
    private final RegistrationService registrationService;
    private final SalleConverters converters;
    private final AuthzBean authz; // para filtrar lo que el usuario actual puede moderar

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/state")
    public ResponseEntity<GlobalStateDto> getGlobalState() {
        boolean locked = academicStateService.isLocked();

        List<UserPending> pendings = registrationService.listPending();
        List<UserPendingDto> dtos = pendings.stream()
                .map(p -> {
                    try {
                        return converters.userPendingToDto(p);
                    } catch (SalleException e) {
                        throw new RuntimeException("Error convirtiendo UserPending -> DTO", e);
                    }
                })
                .toList();

        GlobalStateDto dto = GlobalStateDto.builder()
                .locked(locked)
                .pendings(dtos)
                .build();

        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/lock")
    public ResponseEntity<Void> setLock(@RequestBody GlobalLockRequest req) {
        academicStateService.setLocked(req.isLocked());
        return ResponseEntity.noContent().build();
    }

}