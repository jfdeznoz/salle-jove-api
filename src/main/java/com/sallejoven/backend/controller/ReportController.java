package com.sallejoven.backend.controller;

import com.sallejoven.backend.model.dto.ReportQueueResult;
import com.sallejoven.backend.model.types.ReportType;
import com.sallejoven.backend.service.ReportQueueService;
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

// ReportController.java
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportQueueService reportQueueService;
    private final ReportService reportService;

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<Map<String, Object>> enqueueEventReports(
            @PathVariable Long eventId,
            @RequestParam("types") String typesCsv,
            @RequestParam(defaultValue = "false") boolean overwrite
    ) throws Exception {

        // parsea CSV -> enums
        List<ReportType> types = Arrays.stream(typesCsv.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .map(i -> ReportType.values()[i])
                .collect(Collectors.toList());

        // encola el trabajo
        ReportQueueResult res = reportQueueService.enqueueEventReports(eventId, types, overwrite);

        // 202 Accepted, devolvemos metadatos para que el front pueda consultar el result.json si quiere
        return ResponseEntity.accepted().body(Map.of(
                "jobId", res.jobId(),
                "resultKey", res.resultKey(),         // p.ej. test/jobs/123/result.json
                "outputPrefix", res.outputPrefix(),   // p.ej. 2025/reports/event_39
                "environment", res.environment()      // local | prod
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/general/seguro")
    public ResponseEntity<Map<String, String>> generateGeneralSeguroReport() throws Exception {
        String seguroUrl = reportService.generateSeguroReportZip();
        return ResponseEntity.ok(Map.of("seguroUrl", seguroUrl));
    }
}