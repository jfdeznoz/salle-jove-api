package com.sallejoven.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sallejoven.backend.config.security.SecurityConfig;
import com.sallejoven.backend.model.dto.UserPendingDto;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.requestDto.GlobalLockRequest;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.RegistrationService;
import com.sallejoven.backend.utils.SalleConverters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalController.class)
@Import(GlobalControllerTest.MethodSecurityTestConfig.class)
class GlobalControllerTest {

  @TestConfiguration
  @EnableMethodSecurity(prePostEnabled = true)
  static class MethodSecurityTestConfig { }

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean AcademicStateService academicStateService;
  @MockitoBean RegistrationService registrationService;
  @MockitoBean SalleConverters converters;

  @Test
  @WithMockUser
  void getGlobalState_returns200_andJson() throws Exception {
    when(academicStateService.isLocked()).thenReturn(true);

    var p = new UserPending();
    var dto = new UserPendingDto();
    dto.setId(1L);
    dto.setEmail("user@example.com");

    when(registrationService.listPending()).thenReturn(List.of(p));
    when(converters.userPendingToDto(p)).thenReturn(dto);

    mockMvc.perform(get("/api/global/state"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.locked").value(true))
            .andExpect(jsonPath("$.pendings[0].email").value("user@example.com"));

    verify(academicStateService).isLocked();
    verify(registrationService).listPending();
    verify(converters).userPendingToDto(p);
    verifyNoMoreInteractions(academicStateService, registrationService, converters);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void setLock_returns204_and_callsService() throws Exception {
    var req = new GlobalLockRequest(true);

    mockMvc.perform(put("/api/global/lock")
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNoContent());

    verify(academicStateService).setLocked(true);
    verifyNoMoreInteractions(academicStateService);
  }

  @Test
  void setLock_unauthenticated_returns401_or403() throws Exception {
    // Dependiendo de tu configuración de entry point puede ser 401 (lo más típico) o 403.
    mockMvc.perform(put("/api/global/lock")
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new GlobalLockRequest(true))))
            .andExpect(status().isUnauthorized()); // cambia a isForbidden() si en tu app devuelve 403
  }

  @Test
  @WithMockUser // sin rol ADMIN
  void setLock_withoutAdminRole_returns403() throws Exception {
    mockMvc.perform(put("/api/global/lock")
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new GlobalLockRequest(true))))
            .andExpect(status().isForbidden());
  }
}