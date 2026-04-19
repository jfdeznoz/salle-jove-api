package com.sallejoven.backend.controller;

import com.sallejoven.backend.config.security.AuthzBean;
import com.sallejoven.backend.mapper.UserMapper;
import com.sallejoven.backend.model.dto.UserDto;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.service.UserCenterService;
import com.sallejoven.backend.service.UserGroupService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(UserControllerTest.MethodSecurityTestConfig.class)
class UserControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityTestConfig { }

    @Autowired MockMvc mockMvc;

    @MockitoBean UserService userService;
    @MockitoBean AuthService authService;
    @MockitoBean UserAssembler userAssembler;
    @MockitoBean UserMapper userMapper;
    @MockitoBean GroupService groupService;
    @MockitoBean CenterService centerService;
    @MockitoBean UserGroupService userGroupService;
    @MockitoBean UserCenterService userCenterService;
    @MockitoBean(name = "authz") AuthzBean authz;

    @Test
    @WithMockUser
    void getUsersByGroupId_returnsOnlyCatechumens_forAnimator() throws Exception {
        UUID groupUuid = UUID.randomUUID();
        UserGroup participant = new UserGroup();
        UserDto participantDto = UserDto.builder()
                .uuid(UUID.randomUUID())
                .name("Catecumeno")
                .email("catecumeno@example.com")
                .userType(0)
                .build();

        when(authz.hasGroupRole(eq(groupUuid), eq("ANIMATOR"))).thenReturn(true);
        when(authz.hasCenterOfGroup(eq(groupUuid), eq("PASTORAL_DELEGATE"), eq("GROUP_LEADER"))).thenReturn(false);
        when(userGroupService.findParticipantsByGroupId(groupUuid)).thenReturn(List.of(participant));
        when(userMapper.toUserDtoFromUserGroup(participant)).thenReturn(participantDto);

        mockMvc.perform(get("/api/users/group/{groupUuid}", groupUuid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userType").value(0))
                .andExpect(jsonPath("$[0].name").value("Catecumeno"));

        verify(authz).hasGroupRole(groupUuid, "ANIMATOR");
        verify(authz, times(2)).hasCenterOfGroup(groupUuid, "PASTORAL_DELEGATE", "GROUP_LEADER");
        verify(userGroupService).findParticipantsByGroupId(groupUuid);
        verify(userMapper).toUserDtoFromUserGroup(participant);
        verify(userGroupService, never()).findByGroupId(groupUuid);
        verifyNoMoreInteractions(authz, userGroupService, userMapper, userService, authService, userAssembler, groupService,
                centerService, userCenterService);
    }

    @Test
    @WithMockUser
    void getUsersByGroupId_returnsFullRoster_forGroupLeader() throws Exception {
        UUID groupUuid = UUID.randomUUID();
        UserGroup participant = new UserGroup();
        UserGroup catechist = new UserGroup();
        UserDto participantDto = UserDto.builder()
                .uuid(UUID.randomUUID())
                .name("Catecumeno")
                .email("catecumeno@example.com")
                .userType(0)
                .build();
        UserDto catechistDto = UserDto.builder()
                .uuid(UUID.randomUUID())
                .name("Catequista")
                .email("catequista@example.com")
                .userType(1)
                .build();

        when(authz.hasCenterOfGroup(eq(groupUuid), eq("PASTORAL_DELEGATE"), eq("GROUP_LEADER"))).thenReturn(true);
        when(userGroupService.findByGroupId(groupUuid)).thenReturn(List.of(participant, catechist));
        when(userMapper.toUserDtoFromUserGroup(participant)).thenReturn(participantDto);
        when(userMapper.toUserDtoFromUserGroup(catechist)).thenReturn(catechistDto);

        mockMvc.perform(get("/api/users/group/{groupUuid}", groupUuid))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userType").value(0))
                .andExpect(jsonPath("$[1].userType").value(1));

        verify(authz, times(2)).hasCenterOfGroup(groupUuid, "PASTORAL_DELEGATE", "GROUP_LEADER");
        verify(userGroupService).findByGroupId(groupUuid);
        verify(userMapper).toUserDtoFromUserGroup(participant);
        verify(userMapper).toUserDtoFromUserGroup(catechist);
        verify(userGroupService, never()).findParticipantsByGroupId(groupUuid);
        verifyNoMoreInteractions(authz, userGroupService, userMapper, userService, authService, userAssembler, groupService,
                centerService, userCenterService);
    }
}
