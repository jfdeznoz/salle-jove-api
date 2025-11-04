package com.sallejoven.backend.controller;

import com.sallejoven.backend.model.enums.ReportType;
import com.sallejoven.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<Map<String, List<String>>> generateEventReports(@PathVariable Long eventId, @RequestParam("types") String typesCsv, 
                                                                        @RequestParam(defaultValue = "false") boolean overwrite) throws Exception {
        List<Integer> idxs = Arrays.stream(typesCsv.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        List<ReportType> enumTypes = idxs.stream()
                .map(i -> {
                    try {
                        return ReportType.values()[i];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new IllegalArgumentException("Índice inválido de ReportType: " + i);
                    }
                })
                .collect(Collectors.toList());

        List<String> urls = reportService.generateEventReports(eventId, enumTypes, overwrite);
        return ResponseEntity.ok(Map.of("reportUrls", urls));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/general/seguro")
    public ResponseEntity<Map<String, String>> generateGeneralSeguroReport() throws Exception {
        String seguroUrl = reportService.generateSeguroReportZip();
        return ResponseEntity.ok(Map.of("seguroUrl", seguroUrl));
    }
}