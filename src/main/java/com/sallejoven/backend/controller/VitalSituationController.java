package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.PresignedPutDTO;
import com.sallejoven.backend.model.dto.VitalSituationDto;
import com.sallejoven.backend.model.dto.VitalSituationSessionDto;
import com.sallejoven.backend.model.entity.VitalSituation;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.RequestVitalSituation;
import com.sallejoven.backend.model.requestDto.RequestVitalSituationSession;
import com.sallejoven.backend.repository.VitalSituationSessionRepository;
import com.sallejoven.backend.service.S3V2Service;
import com.sallejoven.backend.service.VitalSituationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.util.List;

@PreAuthorize("isAuthenticated()")
@RequestMapping(value = "/api/vital-situations")
@RestController
@RequiredArgsConstructor
public class VitalSituationController {

    private final VitalSituationService vitalSituationService;
    private final S3V2Service s3v2Service;
    private final VitalSituationSessionRepository vitalSituationSessionRepository;

    @GetMapping
    public ResponseEntity<List<VitalSituationDto>> getAllVitalSituations(
            @RequestParam(required = false) Integer stage) {
        List<VitalSituationDto> situations;
        if (stage != null) {
            situations = vitalSituationService.findByStage(stage);
        } else {
            situations = vitalSituationService.findAll();
        }
        return ResponseEntity.ok(situations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VitalSituationDto> getVitalSituationById(@PathVariable Long id) {
        return vitalSituationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<VitalSituationSessionDto>> getSessionsByVitalSituation(@PathVariable Long id) {
        List<VitalSituationSessionDto> sessions = vitalSituationService.findSessionsByVitalSituationId(id);
        return ResponseEntity.ok(sessions);
    }

    @PreAuthorize("@authz.canCreateOrEditVitalSituation(#request)")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationDto> createVitalSituation(@RequestBody RequestVitalSituation request) {
        VitalSituation vs = vitalSituationService.createVitalSituation(request);
        return ResponseEntity.ok(vitalSituationService.findById(vs.getId()).orElseThrow());
    }

    @PreAuthorize("@authz.canCreateOrEditVitalSituation(#request)")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationDto> updateVitalSituation(
            @PathVariable Long id,
            @RequestBody RequestVitalSituation request) throws SalleException {
        VitalSituation vs = vitalSituationService.updateVitalSituation(id, request);
        return ResponseEntity.ok(vitalSituationService.findById(vs.getId()).orElseThrow());
    }

    @PreAuthorize("@authz.canDeleteVitalSituation(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVitalSituation(@PathVariable Long id) throws SalleException {
        vitalSituationService.deleteVitalSituation(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canCreateOrEditVitalSituationSession(#request)")
    @PostMapping(value = "/sessions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationSessionDto> createVitalSituationSession(
            @RequestBody RequestVitalSituationSession request) throws SalleException {
        VitalSituationSession vss = vitalSituationService.createVitalSituationSession(request);
        return ResponseEntity.ok(vitalSituationService.findSessionById(vss.getId()).orElseThrow());
    }

    @PreAuthorize("@authz.canEditVitalSituationSession(#id)")
    @PutMapping(value = "/sessions/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationSessionDto> updateVitalSituationSession(
            @PathVariable Long id,
            @RequestBody RequestVitalSituationSession request) throws SalleException {
        VitalSituationSession vss = vitalSituationService.updateVitalSituationSession(id, request);
        return ResponseEntity.ok(vitalSituationService.findSessionById(vss.getId()).orElseThrow());
    }

    @PreAuthorize("@authz.canEditVitalSituationSession(#id)")
    @PostMapping(value = "/sessions/{id}/presigned-pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PresignedPutDTO> getPresignedPdfForSession(
            @PathVariable Long id,
            @RequestBody RequestVitalSituationSession request) throws SalleException {
        VitalSituationSession vss = vitalSituationSessionRepository.findById(id)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
        
        try {
            PresignedPutDTO pdfPresigned = s3v2Service.buildPresignedForVitalSituationSessionPdf(
                    vss.getVitalSituation().getId(),
                    vss.getId(),
                    request.getPdfUpload()
            );
            return ResponseEntity.ok(pdfPresigned);
        } catch (SdkClientException e) {
            throw new SalleException(ErrorCodes.AWS_CREDENTIALS_NOT_CONFIGURED,
                    "Por favor, configura las credenciales de AWS para el entorno local.");
        }
    }

    @PreAuthorize("@authz.canEditVitalSituationSession(#id)")
    @PostMapping(value = "/sessions/{id}/finalize-pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationSessionDto> finalizeSessionPdf(
            @PathVariable Long id,
            @RequestBody RequestVitalSituationSession request) throws SalleException {
        String pdfKey = request.getPdfUpload() != null ? request.getPdfUpload().trim() : null;
        VitalSituationSession vss = vitalSituationService.finalizeSessionUpload(id, pdfKey);
        return ResponseEntity.ok(vitalSituationService.findSessionById(vss.getId()).orElseThrow());
    }

    @PreAuthorize("@authz.canDeleteVitalSituationSession(#id)")
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteVitalSituationSession(@PathVariable Long id) throws SalleException {
        vitalSituationService.deleteVitalSituationSession(id);
        return ResponseEntity.noContent().build();
    }
}

