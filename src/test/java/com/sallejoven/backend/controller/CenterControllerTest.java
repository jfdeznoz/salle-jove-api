package com.sallejoven.backend.controller;

import com.sallejoven.backend.config.security.AuthzBean;
import com.sallejoven.backend.mapper.CenterMapper;
import com.sallejoven.backend.model.dto.UserCenterDto;
import com.sallejoven.backend.model.dto.UserCenterGroupsDto;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.UserCenterService;
import com.sallejoven.backend.service.UserService;
import com.sallejoven.backend.service.assembler.UserAssembler;
import java.util.List;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CenterController.class)
@Import(CenterControllerTest.MethodSecurityTestConfig.class)
class CenterControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityTestConfig { }

    @Autowired MockMvc mockMvc;

    @MockitoBean CenterService centerService;
    @MockitoBean UserService userService;
    @MockitoBean AuthService authService;
    @MockitoBean CenterMapper centerMapper;
    @MockitoBean UserAssembler userAssembler;
    @MockitoBean UserCenterService userCenterService;
    @MockitoBean(name = "authz") AuthzBean authz;

    @Test
    @WithMockUser
    void userCenters_returnsVisibleGroups_forAnimatorViewingSharedCatechumen() throws Exception {
        UUID userUuid = UUID.randomUUID();
        UserSalle target = new UserSalle();
        List<UserGroup> memberships = List.of(new UserGroup());
        List<UserCenterGroupsDto> response = List.of(new UserCenterGroupsDto(
                UUID.randomUUID(),
                "La Salle Test",
                "Palma",
                List.of(new UserGroupDto(0, UUID.randomUUID(), UUID.randomUUID(), 3, null)),
                null));

        when(authz.canViewUserGroups(userUuid)).thenReturn(true);
        when(userService.findByUserId(userUuid)).thenReturn(target);
        when(centerService.getActiveUserGroupsForCurrentYear(userUuid)).thenReturn(memberships);
        when(userAssembler.toUserCenterGroupsDtos(memberships)).thenReturn(response);

        mockMvc.perform(get("/api/centers/user/{userId}", userUuid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$[0].centerName").value("La Salle Test"))
                .andExpect(jsonPath("$[0].groups[0].userType").value(0))
                .andExpect(jsonPath("$[0].groups[0].stage").value(3));

        verify(authz).canViewUserGroups(userUuid);
        verify(userService).findByUserId(userUuid);
        verify(centerService).getActiveUserGroupsForCurrentYear(userUuid);
        verify(userAssembler).toUserCenterGroupsDtos(memberships);
        verifyNoMoreInteractions(authz, userService, centerService, userAssembler, authService, centerMapper, userCenterService);
    }

    @Test
    @WithMockUser
    void getUserCenters_returnsCenterRoles_forAnimatorViewingSharedCatechumen() throws Exception {
        UUID userUuid = UUID.randomUUID();
        UserCenter membership = new UserCenter();
        List<UserCenter> memberships = List.of(membership);
        UserCenterDto dto = new UserCenterDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "La Salle Test",
                "Palma",
                2);

        when(authz.canViewUserCenters(userUuid)).thenReturn(true);
        when(userCenterService.findByUserForCurrentYear(userUuid)).thenReturn(memberships);
        when(centerMapper.toUserCenterDto(membership)).thenReturn(dto);

        mockMvc.perform(get("/api/centers/user/{userId}/center", userUuid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$[0].centerName").value("La Salle Test"))
                .andExpect(jsonPath("$[0].userType").value(2));

        verify(authz).canViewUserCenters(userUuid);
        verify(userCenterService).findByUserForCurrentYear(userUuid);
        verify(centerMapper).toUserCenterDto(membership);
        verifyNoMoreInteractions(authz, userCenterService, centerMapper, centerService, userService, userAssembler, authService);
    }

    @Test
    @WithMockUser
    void userCenters_returnsForbidden_whenAnimatorCannotViewTargetUser() throws Exception {
        UUID userUuid = UUID.randomUUID();
        when(authz.canViewUserGroups(userUuid)).thenReturn(false);

        mockMvc.perform(get("/api/centers/user/{userId}", userUuid))
                .andExpect(status().isForbidden());

        verify(authz).canViewUserGroups(userUuid);
        verifyNoMoreInteractions(authz, centerService, userService, authService, centerMapper, userAssembler, userCenterService);
    }
}
