package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.mapper.VitalSituationMapper;
import com.sallejoven.backend.model.dto.VitalSituationDto;
import com.sallejoven.backend.model.dto.VitalSituationSessionDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.VitalSituation;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.VitalSituationEditRequest;
import com.sallejoven.backend.model.requestDto.VitalSituationRequest;
import com.sallejoven.backend.model.requestDto.VitalSituationSessionEditRequest;
import com.sallejoven.backend.model.requestDto.VitalSituationSessionRequest;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.VitalSituationRepository;
import com.sallejoven.backend.repository.VitalSituationSessionRepository;
import com.sallejoven.backend.utils.ReferenceParser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VitalSituationService {

    private final VitalSituationRepository vitalSituationRepository;
    private final VitalSituationSessionRepository vitalSituationSessionRepository;
    private final GroupRepository groupRepository;
    private final S3V2Service s3v2Service;
    private final VitalSituationMapper vitalSituationMapper;
    private final AuthService authService;

    public List<VitalSituationDto> findAll() {
        return vitalSituationRepository.findAllActive().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<VitalSituationDto> findByStage(Integer stage) {
        return vitalSituationRepository.findByStage(stage).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<VitalSituationDto> findByGroupReference(String groupReference) {
        UUID groupUuid = ReferenceParser.asUuid(groupReference)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
        GroupSalle group = groupRepository.findById(groupUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.GROUP_NOT_FOUND));
        return findByStage(group.getStage());
    }

    public Optional<VitalSituationDto> findById(UUID uuid) {
        return vitalSituationRepository.findById(uuid).map(this::toDto);
    }

    public Optional<VitalSituationDto> findByReference(String reference) {
        return ReferenceParser.asUuid(reference).flatMap(vitalSituationRepository::findByUuid).map(this::toDto);
    }

    public Optional<VitalSituation> findEntityByReference(String reference) {
        return ReferenceParser.asUuid(reference).flatMap(vitalSituationRepository::findByUuid);
    }

    public List<VitalSituationSessionDto> findSessionsByVitalSituationId(UUID vitalSituationUuid) {
        return vitalSituationSessionRepository.findByVitalSituationUuid(vitalSituationUuid).stream()
                .map(this::sessionToDto)
                .collect(Collectors.toList());
    }

    public Optional<VitalSituationSessionDto> findSessionById(UUID sessionUuid) {
        return vitalSituationSessionRepository.findById(sessionUuid).map(this::sessionToDto);
    }

    public Optional<VitalSituationSessionDto> findSessionByReference(String reference) {
        return ReferenceParser.asUuid(reference).flatMap(vitalSituationSessionRepository::findByUuid).map(this::sessionToDto);
    }

    public VitalSituationSession findSessionEntityById(UUID sessionUuid) {
        return vitalSituationSessionRepository.findById(sessionUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
    }

    public VitalSituationSession findSessionEntityByReference(String reference) {
        return ReferenceParser.asUuid(reference)
                .flatMap(vitalSituationSessionRepository::findByUuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
    }

    @Transactional
    public VitalSituation createVitalSituation(VitalSituationRequest request) {
        VitalSituation vitalSituation = VitalSituation.builder()
                .title(request.getTitle())
                .stages(request.getStages())
                .isDefault(resolveIsDefaultForCreate(request.getIsDefault()))
                .build();
        return vitalSituationRepository.save(vitalSituation);
    }

    @Transactional
    public VitalSituation updateVitalSituation(UUID uuid, VitalSituationEditRequest request) {
        VitalSituation vitalSituation = vitalSituationRepository.findById(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
        if (request.getTitle() != null) {
            vitalSituation.setTitle(request.getTitle());
        }
        if (request.getStages() != null) {
            vitalSituation.setStages(request.getStages());
        }
        return vitalSituationRepository.save(vitalSituation);
    }

    @Transactional
    public void deleteVitalSituation(UUID uuid) {
        vitalSituationRepository.findById(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
        vitalSituationRepository.softDelete(uuid);
    }

    @Transactional
    public VitalSituationSession createVitalSituationSession(VitalSituationSessionRequest request) {
        VitalSituation vitalSituation = resolveVitalSituation(request.getVitalSituationUuid());
        VitalSituationSession session = VitalSituationSession.builder()
                .vitalSituation(vitalSituation)
                .title(request.getTitle())
                .isDefault(resolveIsDefaultForCreate(request.getIsDefault()))
                .build();
        return vitalSituationSessionRepository.save(session);
    }

    @Transactional
    public VitalSituationSession updateVitalSituationSession(UUID uuid, VitalSituationSessionEditRequest request) {
        VitalSituationSession session = vitalSituationSessionRepository.findById(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }
        if (request.getVitalSituationUuid() != null && !request.getVitalSituationUuid().isBlank()) {
            session.setVitalSituation(resolveVitalSituation(request.getVitalSituationUuid()));
        }
        return vitalSituationSessionRepository.save(session);
    }

    @Transactional
    public VitalSituation setVitalSituationDefaultFlag(UUID uuid, boolean isDefault) {
        ensureCurrentUserIsAdmin();
        VitalSituation vitalSituation = vitalSituationRepository.findById(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
        vitalSituation.setIsDefault(isDefault);
        return vitalSituationRepository.save(vitalSituation);
    }

    @Transactional
    public VitalSituationSession setVitalSituationSessionDefaultFlag(UUID uuid, boolean isDefault) {
        ensureCurrentUserIsAdmin();
        VitalSituationSession session = vitalSituationSessionRepository.findById(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
        session.setIsDefault(isDefault);
        return vitalSituationSessionRepository.save(session);
    }

    @Transactional
    public VitalSituationSession finalizeSessionUpload(UUID uuid, String pdfKey) {
        VitalSituationSession session = vitalSituationSessionRepository.findById(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
        if (pdfKey == null || pdfKey.isBlank()) {
            log.warn("Ignoring blank PDF finalize request for vital situation session {}", uuid);
            return vitalSituationSessionRepository.save(session);
        }

        String oldUrl = session.getPdf();
        if (oldUrl != null && !oldUrl.isBlank()) {
            String oldKey = s3v2Service.keyFromUrl(oldUrl);
            if (oldKey != null && !oldKey.isBlank() && !oldKey.equals(pdfKey)) {
                s3v2Service.deleteObject(oldKey);
            }
        }
        session.setPdf(s3v2Service.publicUrl(pdfKey));
        log.info("Finalized PDF upload for vital situation session {}", uuid);
        return vitalSituationSessionRepository.save(session);
    }

    @Transactional
    public void deleteVitalSituationSession(UUID uuid) {
        VitalSituationSession session = vitalSituationSessionRepository.findById(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_SESSION_NOT_FOUND));
        if (session.getPdf() != null && !session.getPdf().isBlank()) {
            s3v2Service.deleteObject(s3v2Service.keyFromUrl(session.getPdf()));
        }
        vitalSituationSessionRepository.softDelete(uuid);
    }

    private VitalSituationDto toDto(VitalSituation vitalSituation) {
        return vitalSituationMapper.toDto(vitalSituation);
    }

    private VitalSituationSessionDto sessionToDto(VitalSituationSession session) {
        return vitalSituationMapper.toSessionDto(session);
    }

    private VitalSituation resolveVitalSituation(String uuidReference) {
        UUID uuid = ReferenceParser.asUuid(uuidReference)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
        return vitalSituationRepository.findByUuid(uuid)
                .orElseThrow(() -> new SalleException(ErrorCodes.VITAL_SITUATION_NOT_FOUND));
    }

    private Boolean resolveIsDefaultForCreate(Boolean requestedIsDefault) {
        if (isCurrentUserAdmin()) {
            return Boolean.TRUE.equals(requestedIsDefault);
        }
        return false;
    }

    private void ensureCurrentUserIsAdmin() {
        if (isCurrentUserAdmin()) {
            return;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo un admin puede cambiar entre creada y predefinida"
        );
    }

    private boolean isCurrentUserAdmin() {
        return Boolean.TRUE.equals(authService.getCurrentUser().getIsAdmin());
    }
}
