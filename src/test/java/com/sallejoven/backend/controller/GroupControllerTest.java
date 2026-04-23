package com.sallejoven.backend.controller;

import com.sallejoven.backend.config.security.AuthzBean;
import com.sallejoven.backend.mapper.GroupMapper;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.EventService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserGroupService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GroupController.class)
@Import(GroupControllerTest.MethodSecurityTestConfig.class)
class GroupControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityTestConfig { }

    @Autowired MockMvc mockMvc;

    @MockitoBean GroupService groupService;
    @MockitoBean GroupMapper groupMapper;
    @MockitoBean UserGroupService userGroupService;
    @MockitoBean EventService eventService;
    @MockitoBean CenterService centerService;
    @MockitoBean(name = "authz") AuthzBean authz;

    @Test
    @WithMockUser
    void createGroup_returnsForbidden_whenUserIsNotAdmin() throws Exception {
        UUID centerUuid = UUID.randomUUID();

        mockMvc.perform(post("/api/groups/")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "centerUuid": "%s",
                                  "stage": 3
                                }
                                """.formatted(centerUuid)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authz, groupService, groupMapper, userGroupService, eventService, centerService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createGroup_returnsCreatedGroup_whenUserIsAdmin() throws Exception {
        UUID centerUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        Center center = Center.builder()
                .uuid(centerUuid)
                .name("La Salle Test")
                .city("Palma")
                .build();
        GroupSalle saved = GroupSalle.builder()
                .uuid(groupUuid)
                .stage(3)
                .center(center)
                .build();

        when(groupService.createGroup(centerUuid, 3)).thenReturn(saved);

        mockMvc.perform(post("/api/groups/")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "centerUuid": "%s",
                                  "stage": 3
                                }
                                """.formatted(centerUuid)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.uuid").value(groupUuid.toString()))
                .andExpect(jsonPath("$.centerUuid").value(centerUuid.toString()))
                .andExpect(jsonPath("$.centerName").value("La Salle Test"))
                .andExpect(jsonPath("$.stage").value(3));

        verify(groupService).createGroup(centerUuid, 3);
        verifyNoMoreInteractions(groupService);
        verifyNoInteractions(authz, groupMapper, userGroupService, eventService, centerService);
    }
}
