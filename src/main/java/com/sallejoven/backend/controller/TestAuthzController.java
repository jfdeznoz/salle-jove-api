package com.sallejoven.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestAuthzController {

    // 1) Ver qué authorities llegan desde el JWT (roles + authz)
    @GetMapping("/authorities")
    public List<String> authorities() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    // 2) Debe devolver 200 si el token tiene CENTER:{centerId}:PASTORAL_DELEGATE:{year} o GROUP_LEADER
    @PreAuthorize("@authz.hasCenterRole(#centerId, 'PASTORAL_DELEGATE','GROUP_LEADER')")
    @GetMapping("/centers/{centerId}/ping")
    public ResponseEntity<String> pingCenter(@PathVariable Long centerId) {
        return ResponseEntity.ok("ok center " + centerId);
    }

    // 3) Debe devolver 200 si el token tiene GROUP:{groupId}:ANIMATOR:{year}
    @PreAuthorize("@authz.hasGroupRole(#groupId, 'ANIMATOR')")
    @GetMapping("/groups/{groupId}/ping")
    public ResponseEntity<String> pingGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok("ok group " + groupId);
    }
}