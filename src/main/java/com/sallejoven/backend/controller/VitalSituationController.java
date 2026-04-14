package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.PresignedPutDTO;
import com.sallejoven.backend.model.dto.VitalSituationDto;
import com.sallejoven.backend.model.dto.VitalSituationSessionDto;
import com.sallejoven.backend.model.entity.VitalSituation;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.VitalSituationEditRequest;
import com.sallejoven.backend.model.requestDto.VitalSituationRequest;
import com.sallejoven.backend.model.requestDto.VitalSituationSessionEditRequest;
import com.sallejoven.backend.model.requestDto.VitalSituationSessionFinalizePdfRequest;
import com.sallejoven.backend.model.requestDto.VitalSituationSessionPresignedPdfRequest;
import com.sallejoven.backend.model.requestDto.VitalSituationSessionRequest;
import com.sallejoven.backend.service.S3V2Service;
import com.sallejoven.backend.service.VitalSituationService;
import com.sallejoven.backend.utils.ReferenceParser;
import jakarta.validation.Valid;
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
import java.util.UUID;

@PreAuthorize("isAuthenticated()")
@RequestMapping(value = "/api/vital-situations")
@RestController
@RequiredArgsConstructor
public class VitalSituationController {

    private final VitalSituationService vitalSituationService;
    private final S3V2Service s3v2Service;

    @GetMapping
    public ResponseEntity<List<VitalSituationDto>> getAllVitalSituations(
            @RequestParam(required = false) Integer stage) {
        List<VitalSituationDto> situations = stage != null
                ? vitalSituationService.findByStage(stage)
                : vitalSituationService.findAll();
        return ResponseEntity.ok(situations);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<VitalSituationDto> getVitalSituationById(@PathVariable String uuid) {
        return vitalSituationService.findByReference(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{uuid}/sessions")
    public ResponseEntity<List<VitalSituationSessionDto>> getSessionsByVitalSituation(@PathVariable String uuid) {
        UUID resolved = requireUuid(uuid, ErrorCodes.VITAL_SITUATION_NOT_FOUND);
        vitalSituationService.findById(resolved)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
        return ResponseEntity.ok(vitalSituationService.findSessionsByVitalSituationId(resolved));
    }

    @PreAuthorize("@authz.canCreateOrEditVitalSituation(#request)")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationDto> createVitalSituation(@Valid @RequestBody VitalSituationRequest request) {
        VitalSituation vs = vitalSituationService.createVitalSituation(request);
        return ResponseEntity.ok(vitalSituationService.findById(vs.getUuid()).orElseThrow());
    }

    @PreAuthorize("@authz.canEditVitalSituation(#uuid)")
    @PutMapping(value = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationDto> updateVitalSituation(
            @PathVariable UUID uuid,
            @Valid @RequestBody VitalSituationEditRequest request) {
        VitalSituation vs = vitalSituationService.updateVitalSituation(uuid, request);
        return ResponseEntity.ok(vitalSituationService.findById(vs.getUuid()).orElseThrow());
    }

    @PreAuthorize("@authz.canDeleteVitalSituation(#uuid)")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteVitalSituation(@PathVariable UUID uuid) {
        vitalSituationService.deleteVitalSituation(uuid);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authz.canCreateOrEditVitalSituationSession(#request)")
    @PostMapping(value = "/sessions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationSessionDto> createVitalSituationSession(
            @Valid @RequestBody VitalSituationSessionRequest request) {
        VitalSituationSession vss = vitalSituationService.createVitalSituationSession(request);
        return ResponseEntity.ok(vitalSituationService.findSessionById(vss.getUuid()).orElseThrow());
    }

    @PreAuthorize("@authz.canEditVitalSituationSession(#uuid)")
    @PutMapping(value = "/sessions/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationSessionDto> updateVitalSituationSession(
            @PathVariable UUID uuid,
            @Valid @RequestBody VitalSituationSessionEditRequest request) {
        VitalSituationSession vss = vitalSituationService.updateVitalSituationSession(uuid, request);
        return ResponseEntity.ok(vitalSituationService.findSessionById(vss.getUuid()).orElseThrow());
    }

    @PreAuthorize("@authz.canEditVitalSituationSession(#uuid)")
    @PostMapping(value = "/sessions/{uuid}/presigned-pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PresignedPutDTO> getPresignedPdfForSession(
            @PathVariable UUID uuid,
            @Valid @RequestBody VitalSituationSessionPresignedPdfRequest request) {
        VitalSituationSession vss = vitalSituationService.findSessionEntityById(uuid);
        try {
            PresignedPutDTO pdfPresigned = s3v2Service.buildPresignedForVitalSituationSessionPdf(
                    vss.getVitalSituation().getUuid(),
                    vss.getUuid(),
                    request.pdfOriginalName()
            );
            return ResponseEntity.ok(pdfPresigned);
        } catch (SdkClientException e) {
            throw new SalleException(ErrorCodes.AWS_CREDENTIALS_NOT_CONFIGURED,
                    "Por favor, configura las credenciales de AWS para el entorno local.");
        }
    }

    @PreAuthorize("@authz.canEditVitalSituationSession(#uuid)")
    @PostMapping(value = "/sessions/{uuid}/finalize-pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VitalSituationSessionDto> finalizeSessionPdf(
            @PathVariable UUID uuid,
            @Valid @RequestBody VitalSituationSessionFinalizePdfRequest request) {
        String pdfKey = request.pdfKey() != null ? request.pdfKey().trim() : null;
        VitalSituationSession vss = vitalSituationService.finalizeSessionUpload(uuid, pdfKey);
        return ResponseEntity.ok(vitalSituationService.findSessionById(vss.getUuid()).orElseThrow());
    }

    @PreAuthorize("@authz.canDeleteVitalSituationSession(#uuid)")
    @DeleteMapping("/sessions/{uuid}")
    public ResponseEntity<Void> deleteVitalSituationSession(@PathVariable UUID uuid) {
        vitalSituationService.deleteVitalSituationSession(uuid);
        return ResponseEntity.noContent().build();
    }

    private UUID requireUuid(String reference, ErrorCodes notFound) {
        return ReferenceParser.asUuid(reference).orElseThrow(() -> new SalleException(notFound));
    }
}
