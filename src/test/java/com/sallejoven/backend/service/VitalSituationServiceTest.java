package com.sallejoven.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sallejoven.backend.mapper.VitalSituationMapper;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.VitalSituation;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import com.sallejoven.backend.model.requestDto.VitalSituationRequest;
import com.sallejoven.backend.model.requestDto.VitalSituationSessionRequest;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.VitalSituationRepository;
import com.sallejoven.backend.repository.VitalSituationSessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VitalSituationServiceTest {

    @Mock VitalSituationRepository vitalSituationRepository;
    @Mock VitalSituationSessionRepository vitalSituationSessionRepository;
    @Mock GroupRepository groupRepository;
    @Mock S3V2Service s3v2Service;
    @Mock VitalSituationMapper vitalSituationMapper;
    @Mock AuthService authService;

    @InjectMocks VitalSituationService vitalSituationService;

    @Test
    void createVitalSituation_forcesCreatedType_forNonAdmin() {
        when(authService.getCurrentUser()).thenReturn(user(false));
        when(vitalSituationRepository.save(any(VitalSituation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VitalSituation created = vitalSituationService.createVitalSituation(
                VitalSituationRequest.builder()
                        .title("Nueva")
                        .stages(new Integer[]{0, 1})
                        .isDefault(true)
                        .build()
        );

        assertThat(created.getIsDefault()).isFalse();
    }

    @Test
    void createVitalSituation_allowsPredefinedType_forAdmin() {
        when(authService.getCurrentUser()).thenReturn(user(true));
        when(vitalSituationRepository.save(any(VitalSituation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VitalSituation created = vitalSituationService.createVitalSituation(
                VitalSituationRequest.builder()
                        .title("Nueva")
                        .stages(new Integer[]{0, 1})
                        .isDefault(true)
                        .build()
        );

        assertThat(created.getIsDefault()).isTrue();
    }

    @Test
    void createVitalSituationSession_forcesCreatedType_forNonAdmin() {
        UUID vitalSituationUuid = UUID.randomUUID();
        VitalSituation vitalSituation = VitalSituation.builder()
                .uuid(vitalSituationUuid)
                .title("Padre")
                .stages(new Integer[]{0, 1})
                .build();

        when(authService.getCurrentUser()).thenReturn(user(false));
        when(vitalSituationRepository.findByUuid(vitalSituationUuid)).thenReturn(Optional.of(vitalSituation));
        when(vitalSituationSessionRepository.save(any(VitalSituationSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VitalSituationSession created = vitalSituationService.createVitalSituationSession(
                VitalSituationSessionRequest.builder()
                        .vitalSituationUuid(vitalSituationUuid.toString())
                        .title("Sesion")
                        .isDefault(true)
                        .build()
        );

        assertThat(created.getIsDefault()).isFalse();
    }

    @Test
    void setVitalSituationDefaultFlag_throwsForbidden_forNonAdmin() {
        when(authService.getCurrentUser()).thenReturn(user(false));

        assertThatThrownBy(() -> vitalSituationService.setVitalSituationDefaultFlag(UUID.randomUUID(), true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void setVitalSituationDefaultFlag_updatesValue_forAdmin() {
        UUID uuid = UUID.randomUUID();
        VitalSituation existing = VitalSituation.builder()
                .uuid(uuid)
                .title("Situacion")
                .stages(new Integer[]{0, 1})
                .isDefault(false)
                .build();

        when(authService.getCurrentUser()).thenReturn(user(true));
        when(vitalSituationRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(vitalSituationRepository.save(any(VitalSituation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VitalSituation updated = vitalSituationService.setVitalSituationDefaultFlag(uuid, true);

        assertThat(updated.getIsDefault()).isTrue();
    }

    @Test
    void setVitalSituationSessionDefaultFlag_updatesValue_forAdmin() {
        UUID uuid = UUID.randomUUID();
        VitalSituationSession existing = VitalSituationSession.builder()
                .uuid(uuid)
                .title("Sesion")
                .isDefault(false)
                .vitalSituation(VitalSituation.builder()
                        .uuid(UUID.randomUUID())
                        .title("Padre")
                        .stages(new Integer[]{0, 1})
                        .build())
                .build();

        when(authService.getCurrentUser()).thenReturn(user(true));
        when(vitalSituationSessionRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(vitalSituationSessionRepository.save(any(VitalSituationSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VitalSituationSession updated = vitalSituationService.setVitalSituationSessionDefaultFlag(uuid, true);

        assertThat(updated.getIsDefault()).isTrue();
    }

    private UserSalle user(boolean isAdmin) {
        return UserSalle.builder()
                .uuid(UUID.randomUUID())
                .email("test@sallejoven.com")
                .name("Test")
                .lastName("User")
                .dni("00000000A")
                .phone("600000000")
                .tshirtSize(0)
                .healthCardNumber("HC")
                .imageAuthorization(false)
                .birthDate(new java.util.Date())
                .isAdmin(isAdmin)
                .build();
    }
}
