package com.sallejoven.backend.controller;

import com.sallejoven.backend.config.security.AuthzBean;
import com.sallejoven.backend.model.dto.PresignedPutDTO;
import com.sallejoven.backend.model.dto.VitalSituationDto;
import com.sallejoven.backend.model.dto.VitalSituationSessionDto;
import com.sallejoven.backend.model.entity.VitalSituation;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import com.sallejoven.backend.service.S3V2Service;
import com.sallejoven.backend.service.VitalSituationService;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VitalSituationController.class)
@Import(VitalSituationControllerTest.MethodSecurityTestConfig.class)
class VitalSituationControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityTestConfig { }

    @Autowired MockMvc mockMvc;

    @MockitoBean VitalSituationService vitalSituationService;
    @MockitoBean S3V2Service s3v2Service;
    @MockitoBean(name = "authz") AuthzBean authz;

    @Test
    @WithMockUser
    void getAllVitalSituations_returnsForbidden_whenReadAuthorizationFails() throws Exception {
        when(authz.canViewVitalSituations(null)).thenReturn(false);

        mockMvc.perform(get("/api/vital-situations"))
                .andExpect(status().isForbidden());

        verify(authz).canViewVitalSituations(null);
        verifyNoMoreInteractions(authz, vitalSituationService, s3v2Service);
    }

    @Test
    @WithMockUser
    void getAllVitalSituations_filtersByGroupUuid_whenProvided() throws Exception {
        UUID groupUuid = UUID.randomUUID();
        UUID vsUuid = UUID.randomUUID();
        when(authz.canViewVitalSituations(groupUuid.toString())).thenReturn(true);
        when(vitalSituationService.findByGroupReference(groupUuid.toString())).thenReturn(List.of(
                new VitalSituationDto(vsUuid, "Jerusalen", new Integer[]{8}, false)));

        mockMvc.perform(get("/api/vital-situations").param("groupUuid", groupUuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(vsUuid.toString()))
                .andExpect(jsonPath("$[0].stages[0]").value(8));

        verify(authz).canViewVitalSituations(groupUuid.toString());
        verify(vitalSituationService).findByGroupReference(groupUuid.toString());
        verifyNoMoreInteractions(authz, vitalSituationService, s3v2Service);
    }

    @Test
    @WithMockUser
    void getVitalSituationById_returnsForbidden_whenReadAuthorizationFails() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(authz.canViewVitalSituation(uuid.toString())).thenReturn(false);

        mockMvc.perform(get("/api/vital-situations/{uuid}", uuid))
                .andExpect(status().isForbidden());

        verify(authz).canViewVitalSituation(uuid.toString());
        verifyNoMoreInteractions(authz, vitalSituationService, s3v2Service);
    }

    @Test
    @WithMockUser
    void getSessionsByVitalSituation_returnsForbidden_whenReadAuthorizationFails() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(authz.canViewVitalSituationSession(uuid.toString())).thenReturn(false);

        mockMvc.perform(get("/api/vital-situations/{uuid}/sessions", uuid))
                .andExpect(status().isForbidden());

        verify(authz).canViewVitalSituationSession(uuid.toString());
        verifyNoMoreInteractions(authz, vitalSituationService, s3v2Service);
    }

    @Test
    @WithMockUser
    void updateVitalSituation_allowsPartialPayload_withoutTitle() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(authz.canEditVitalSituation(uuid)).thenReturn(true);
        when(vitalSituationService.updateVitalSituation(any(), any())).thenReturn(
                VitalSituation.builder().uuid(uuid).title("Acompanamiento").stages(new Integer[]{1, 2}).build());
        when(vitalSituationService.findById(uuid)).thenReturn(Optional.of(
                new VitalSituationDto(uuid, "Acompanamiento", new Integer[]{1, 2}, false)));

        mockMvc.perform(put("/api/vital-situations/{uuid}", uuid)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stages":[1,2]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Acompanamiento"))
                .andExpect(jsonPath("$.stages[0]").value(1));

        verify(authz).canEditVitalSituation(uuid);
        verify(vitalSituationService).updateVitalSituation(any(), any());
        verify(vitalSituationService).findById(uuid);
        verifyNoMoreInteractions(authz, vitalSituationService, s3v2Service);
    }

    @Test
    @WithMockUser
    void updateVitalSituation_returnsForbidden_whenEditAuthorizationFails() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(authz.canEditVitalSituation(uuid)).thenReturn(false);

        mockMvc.perform(put("/api/vital-situations/{uuid}", uuid)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"stages":[2]}
                                """))
                .andExpect(status().isForbidden());

        verify(authz).canEditVitalSituation(uuid);
        verifyNoMoreInteractions(authz, vitalSituationService, s3v2Service);
    }

    @Test
    @WithMockUser
    void updateVitalSituationSession_allowsPartialPayload_withoutTitle() throws Exception {
        UUID sessionUuid = UUID.randomUUID();
        UUID vitalSituationUuid = UUID.randomUUID();
        when(authz.canEditVitalSituationSession(sessionUuid)).thenReturn(true);
        when(vitalSituationService.updateVitalSituationSession(any(), any())).thenReturn(
                VitalSituationSession.builder().uuid(sessionUuid).title("Sesion vital").build());
        when(vitalSituationService.findSessionById(sessionUuid)).thenReturn(Optional.of(
                new VitalSituationSessionDto(sessionUuid, vitalSituationUuid, "Sesion vital", null, false)));

        mockMvc.perform(put("/api/vital-situations/sessions/{uuid}", sessionUuid)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"vitalSituationUuid":"%s"}
                                """.formatted(vitalSituationUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sesion vital"));

        verify(authz).canEditVitalSituationSession(sessionUuid);
        verify(vitalSituationService).updateVitalSituationSession(any(), any());
        verify(vitalSituationService).findSessionById(sessionUuid);
        verifyNoMoreInteractions(authz, vitalSituationService, s3v2Service);
    }

    @Test
    @WithMockUser
    void getPresignedPdf_acceptsLegacyPdfUploadField_withoutTitle() throws Exception {
        UUID sessionUuid = UUID.randomUUID();
        UUID vitalSituationUuid = UUID.randomUUID();
        when(authz.canEditVitalSituationSession(sessionUuid)).thenReturn(true);
        when(vitalSituationService.findSessionEntityById(sessionUuid)).thenReturn(
                VitalSituationSession.builder()
                        .uuid(sessionUuid)
                        .title("Sesion vital")
                        .vitalSituation(VitalSituation.builder().uuid(vitalSituationUuid).title("VS").build())
                        .build());
        when(s3v2Service.buildPresignedForVitalSituationSessionPdf(vitalSituationUuid, sessionUuid, "informe.pdf"))
                .thenReturn(new PresignedPutDTO(
                        "https://bucket.example/upload",
                        "uploads/session.pdf",
                        Map.of("Content-Type", "application/pdf")));

        mockMvc.perform(post("/api/vital-situations/sessions/{uuid}/presigned-pdf", sessionUuid)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"pdfUpload":"informe.pdf"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("uploads/session.pdf"));

        verify(authz).canEditVitalSituationSession(sessionUuid);
        verify(vitalSituationService).findSessionEntityById(sessionUuid);
        verify(s3v2Service).buildPresignedForVitalSituationSessionPdf(vitalSituationUuid, sessionUuid, "informe.pdf");
        verifyNoMoreInteractions(authz, vitalSituationService, s3v2Service);
    }

    @Test
    @WithMockUser
    void finalizePdf_acceptsLegacyPdfUploadAlias_asPdfKey() throws Exception {
        UUID sessionUuid = UUID.randomUUID();
        UUID vitalSituationUuid = UUID.randomUUID();
        when(authz.canEditVitalSituationSession(sessionUuid)).thenReturn(true);
        when(vitalSituationService.finalizeSessionUpload(sessionUuid, "uploads/final.pdf")).thenReturn(
                VitalSituationSession.builder().uuid(sessionUuid).title("Sesion vital").build());
        when(vitalSituationService.findSessionById(sessionUuid)).thenReturn(Optional.of(
                new VitalSituationSessionDto(
                        sessionUuid,
                        vitalSituationUuid,
                        "Sesion vital",
                        "https://cdn.example/uploads/final.pdf",
                        false)));

        mockMvc.perform(post("/api/vital-situations/sessions/{uuid}/finalize-pdf", sessionUuid)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"pdfUpload":"uploads/final.pdf"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdf").value("https://cdn.example/uploads/final.pdf"));

        verify(authz).canEditVitalSituationSession(sessionUuid);
        verify(vitalSituationService).finalizeSessionUpload(sessionUuid, "uploads/final.pdf");
        verify(vitalSituationService).findSessionById(sessionUuid);
        verifyNoMoreInteractions(authz, vitalSituationService, s3v2Service);
    }
}
