package com.sallejoven.backend.controller;

import com.sallejoven.backend.model.dto.ReportQueueResult;
import com.sallejoven.backend.model.enums.ReportType;
import com.sallejoven.backend.service.ReportQueueService;
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

    private final ReportQueueService reportQueueService;

    @PreAuthorize("@authz.canManageEventForEditOrDelete(#eventId)")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<Map<String, Object>> enqueueEventReports(
            @PathVariable Long eventId,
            @RequestParam("types") String typesCsv,
            @RequestParam(defaultValue = "false") boolean overwrite
    ) throws Exception {

        List<ReportType> types = Arrays.stream(typesCsv.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .map(i -> ReportType.values()[i])
                .collect(Collectors.toList());

        ReportQueueResult res = reportQueueService.enqueueEventReports(eventId, types, overwrite);

        return ResponseEntity.accepted().body(Map.of(
                "jobId", res.jobId(),
                "resultKey", res.resultKey(),
                "outputPrefix", res.outputPrefix(),
                "environment", res.environment()
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/general/seguro")
    public ResponseEntity<Map<String, Object>> enqueueGeneralSeguroReport() throws Exception {
        var res = reportQueueService.enqueueGeneralSeguroReport();
        return ResponseEntity.accepted().body(Map.of(
                "jobId",        res.jobId(),
                "resultKey",    res.resultKey(),
                "outputPrefix", res.outputPrefix(),
                "environment",  res.environment()
        ));
    }
}