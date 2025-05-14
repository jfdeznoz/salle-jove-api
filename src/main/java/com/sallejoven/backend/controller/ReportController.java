package com.sallejoven.backend.controller;

import com.sallejoven.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/event/{eventId}")
    public ResponseEntity<Map<String, String>> generateEventReports(@PathVariable Long eventId) throws Exception {
        String zipUrl = reportService.generateInfoAndTshirtReportZip(eventId);
        return ResponseEntity.ok(Map.of("zipUrl", zipUrl));
    }

    @GetMapping("/general/seguro")
    public ResponseEntity<Map<String, String>> generateGeneralSeguroReport() throws Exception {
        String seguroUrl = reportService.generateSeguroReportZip();
        return ResponseEntity.ok(Map.of("seguroUrl", seguroUrl));
    }
}