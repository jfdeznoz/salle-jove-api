package com.sallejoven.backend.controller;

import com.sallejoven.backend.model.dto.AdminOverviewDto;
import com.sallejoven.backend.model.dto.CenterAttendanceStatsDto;
import com.sallejoven.backend.model.dto.GroupAttendanceStatsDto;
import com.sallejoven.backend.model.dto.UserAttendanceStatsDto;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.StatsService;
import com.sallejoven.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final UserService userService;
    private final GroupService groupService;
    private final CenterService centerService;

    @PreAuthorize("@authz.canViewUserStats(#userReference)")
    @GetMapping("/user/{userReference}/attendance")
    public ResponseEntity<UserAttendanceStatsDto> getUserAttendanceStats(@PathVariable String userReference,
                                                                         @RequestParam(required = false) Integer year) {
        UUID userUuid = userService.findByReference(userReference).getUuid();
        return ResponseEntity.ok(statsService.getUserAttendanceStats(userUuid, year));
    }

    @PreAuthorize("@authz.canViewUserStats(#userReference)")
    @GetMapping("/user/{userReference}/years")
    public ResponseEntity<List<Integer>> getUserAvailableYears(@PathVariable String userReference) {
        UUID userUuid = userService.findByReference(userReference).getUuid();
        return ResponseEntity.ok(statsService.getAvailableYears(userUuid));
    }

    @PreAuthorize("@authz.isAnyManagerType()")
    @GetMapping("/group/{groupReference}/attendance")
    public ResponseEntity<GroupAttendanceStatsDto> getGroupAttendanceStats(@PathVariable String groupReference,
                                                                           @RequestParam(required = false) Integer year) {
        UUID groupUuid = groupService.findByReference(groupReference).getUuid();
        return ResponseEntity.ok(statsService.getGroupAttendanceStats(groupUuid, year));
    }

    @PreAuthorize("@authz.isAnyManagerType()")
    @GetMapping("/center/{centerReference}/attendance")
    public ResponseEntity<CenterAttendanceStatsDto> getCenterAttendanceStats(@PathVariable String centerReference,
                                                                             @RequestParam(required = false) Integer year) {
        UUID centerUuid = centerService.findByReference(centerReference).getUuid();
        return ResponseEntity.ok(statsService.getCenterAttendanceStats(centerUuid, year));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/overview")
    public ResponseEntity<AdminOverviewDto> getAdminOverview(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(statsService.getAdminOverview(year));
    }
}
