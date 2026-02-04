package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.VitalSituationDto;
import com.sallejoven.backend.model.dto.VitalSituationSessionDto;
import com.sallejoven.backend.model.entity.VitalSituation;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.RequestVitalSituation;
import com.sallejoven.backend.model.requestDto.RequestVitalSituationSession;
import com.sallejoven.backend.repository.VitalSituationRepository;
import com.sallejoven.backend.repository.VitalSituationSessionRepository;
import com.sallejoven.backend.service.S3V2Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VitalSituationService {

    private final VitalSituationRepository vitalSituationRepository;
    private final VitalSituationSessionRepository vitalSituationSessionRepository;
    private final S3V2Service s3v2Service;

    public List<VitalSituationDto> findAll() {
        return vitalSituationRepository.findAllActive().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<VitalSituationDto> findByStage(Integer stage) {
        List<VitalSituation> found = vitalSituationRepository.findByStage(stage);
        System.out.println("🔍 Buscando situaciones vitales para stage: " + stage + ", encontradas: " + found.size());
        for (VitalSituation vs : found) {
            System.out.println("  - " + vs.getTitle() + ": stages=" + java.util.Arrays.toString(vs.getStages()));
        }
        return found.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Optional<VitalSituationDto> findById(Long id) {
        return vitalSituationRepository.findById(id)
                .map(this::toDto);
    }

    public List<VitalSituationSessionDto> findSessionsByVitalSituationId(Long vitalSituationId) {
        return vitalSituationSessionRepository.findByVitalSituationId(vitalSituationId).stream()
                .map(this::sessionToDto)
                .collect(Collectors.toList());
    }

    public Optional<VitalSituationSessionDto> findSessionById(Long sessionId) {
        return vitalSituationSessionRepository.findById(sessionId)
                .map(this::sessionToDto);
    }

    @Transactional
    public VitalSituation createVitalSituation(RequestVitalSituation request) {
        VitalSituation vs = VitalSituation.builder()
                .title(request.getTitle())
                .stages(request.getStages())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();
        return vitalSituationRepository.save(vs);
    }

    @Transactional
    public VitalSituation updateVitalSituation(Long id, RequestVitalSituation request) throws SalleException {
        VitalSituation vs = vitalSituationRepository.findById(id)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
        
        if (request.getTitle() != null) {
            vs.setTitle(request.getTitle());
        }
        if (request.getStages() != null) {
            vs.setStages(request.getStages());
        }
        if (request.getIsDefault() != null) {
            vs.setIsDefault(request.getIsDefault());
        }
        
        return vitalSituationRepository.save(vs);
    }

    @Transactional
    public void deleteVitalSituation(Long id) throws SalleException {
        VitalSituation vs = vitalSituationRepository.findById(id)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
        vitalSituationRepository.softDelete(id);
    }

    @Transactional
    public VitalSituationSession createVitalSituationSession(RequestVitalSituationSession request) throws SalleException {
        VitalSituation vs = vitalSituationRepository.findById(request.getVitalSituationId())
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
        
        VitalSituationSession vss = VitalSituationSession.builder()
                .vitalSituation(vs)
                .title(request.getTitle())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();
        return vitalSituationSessionRepository.save(vss);
    }

    @Transactional
    public VitalSituationSession updateVitalSituationSession(Long id, RequestVitalSituationSession request) throws SalleException {
        VitalSituationSession vss = vitalSituationSessionRepository.findById(id)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
        
        if (request.getTitle() != null) {
            vss.setTitle(request.getTitle());
        }
        if (request.getVitalSituationId() != null) {
            VitalSituation vs = vitalSituationRepository.findById(request.getVitalSituationId())
                    .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
            vss.setVitalSituation(vs);
        }
        if (request.getIsDefault() != null) {
            vss.setIsDefault(request.getIsDefault());
        }
        
        return vitalSituationSessionRepository.save(vss);
    }

    @Transactional
    public VitalSituationSession finalizeSessionUpload(Long id, String pdfKey) throws SalleException {
        VitalSituationSession vss = vitalSituationSessionRepository.findById(id)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
        
        if (pdfKey != null && !pdfKey.isBlank()) {
            String oldUrl = vss.getPdf();
            if (oldUrl != null && !oldUrl.isBlank()) {
                String oldKey = s3v2Service.keyFromUrl(oldUrl);
                if (oldKey != null && !oldKey.isBlank() && !oldKey.equals(pdfKey)) {
                    s3v2Service.deleteObject(oldKey);
                }
            }
            vss.setPdf(s3v2Service.publicUrl(pdfKey));
        }
        return vitalSituationSessionRepository.save(vss);
    }

    @Transactional
    public void deleteVitalSituationSession(Long id) throws SalleException {
        VitalSituationSession vss = vitalSituationSessionRepository.findById(id)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
        
        if (vss.getPdf() != null && !vss.getPdf().isBlank()) {
            s3v2Service.deleteObject(s3v2Service.keyFromUrl(vss.getPdf()));
        }
        vitalSituationSessionRepository.softDelete(id);
    }

    private VitalSituationDto toDto(VitalSituation vs) {
        return VitalSituationDto.builder()
                .id(vs.getId())
                .title(vs.getTitle())
                .stages(vs.getStages())
                .isDefault(vs.getIsDefault())
                .build();
    }

    private VitalSituationSessionDto sessionToDto(VitalSituationSession vss) {
        return VitalSituationSessionDto.builder()
                .id(vss.getId())
                .vitalSituationId(vss.getVitalSituation().getId())
                .title(vss.getTitle())
                .pdf(vss.getPdf())
                .isDefault(vss.getIsDefault())
                .build();
    }
}

