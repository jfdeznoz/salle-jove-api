package com.sallejoven.backend.controller;

import com.sallejoven.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{eventId}")
    public ResponseEntity<Map<String, String>> generateReports(@PathVariable Long eventId) throws Exception {
        String zipUrl = reportService.generateReports(eventId);
        return ResponseEntity.ok(Map.of("zipUrl", zipUrl));
    }

}