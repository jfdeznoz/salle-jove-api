package com.sallejoven.backend.config.security;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.enums.ErrorCodes;
import java.time.LocalDateTime;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.repository.EventGroupRepository;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserPendingRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.AuthorityService;
import com.sallejoven.backend.service.EventService;
import com.sallejoven.backend.service.VitalSituationService;
import com.sallejoven.backend.service.WeeklySessionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthzBeanTest {

    @Mock UserCenterRepository userCenterRepo;
    @Mock UserRepository userRepo;
    @Mock UserGroupRepository userGroupRepo;
    @Mock CenterRepository centerRepo;
    @Mock GroupRepository groupRepository;
    @Mock AcademicStateService academicStateService;
    @Mock EventService eventService;
    @Mock EventGroupRepository eventGroupRepo;
    @Mock AuthorityService authorityService;
    @Mock UserPendingRepository userPendingRepo;
    @Mock WeeklySessionService weeklySessionService;
    @Mock VitalSituationService vitalSituationService;

    @InjectMocks AuthzBean authzBean;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void canViewUserStats_returnsTrue_forSelf() {
        UUID userUuid = UUID.randomUUID();
        UserSalle currentUser = new UserSalle();
        currentUser.setUuid(userUuid);

        when(userRepo.findByUuid(userUuid)).thenReturn(Optional.of(currentUser));
        when(userRepo.findByEmail("self@example.com")).thenReturn(Optional.of(currentUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("self@example.com", "N/A"));

        boolean allowed = authzBean.canViewUserStats(userUuid.toString());

        assertThat(allowed).isTrue();
        verify(userRepo).findByUuid(userUuid);
        verify(userRepo).findByEmail("self@example.com");
        verifyNoMoreInteractions(userRepo, userGroupRepo, userCenterRepo, academicStateService, authorityService);
    }

    @Test
    void canViewUserStats_returnsTrue_forGroupLeaderWithVisibility() {
        UUID actorUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        UserSalle actor = new UserSalle();
        actor.setUuid(actorUuid);

        UserSalle target = new UserSalle();
        target.setUuid(targetUuid);

        Center center = new Center();
        center.setUuid(centerUuid);

        GroupSalle group = new GroupSalle();
        group.setUuid(groupUuid);
        group.setCenter(center);

        UserGroup membership = new UserGroup();
        membership.setGroup(group);

        when(userRepo.findByUuid(targetUuid)).thenReturn(Optional.of(target));
        when(userRepo.findByEmail("leader@example.com")).thenReturn(Optional.of(actor));
        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNull(targetUuid, 2025)).thenReturn(List.of(membership));
        when(userCenterRepo.findByUser_UuidAndYearAndDeletedAtIsNull(targetUuid, 2025)).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "leader@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("CENTER:" + centerUuid + ":GROUP_LEADER:2025"))));

        boolean allowed = authzBean.canViewUserStats(targetUuid.toString());

        assertThat(allowed).isTrue();
        verify(userRepo).findByUuid(targetUuid);
        verify(userRepo).findByEmail("leader@example.com");
        verify(academicStateService).getVisibleYear();
        verify(userGroupRepo).findByUser_UuidAndYearAndDeletedAtIsNull(targetUuid, 2025);
        verify(userCenterRepo).findByUser_UuidAndYearAndDeletedAtIsNull(targetUuid, 2025);
        verifyNoMoreInteractions(userRepo, userGroupRepo, userCenterRepo, academicStateService, authorityService);
    }

    @Test
    void canViewUserStats_throwsUserNotFound_forUnknownReference() {
        assertThatThrownBy(() -> authzBean.canViewUserStats("not-a-uuid"))
                .isInstanceOfSatisfying(SalleException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCodes.USER_NOT_FOUND.getErrorCode()));

        verifyNoMoreInteractions(userRepo, userGroupRepo, userCenterRepo, academicStateService, authorityService);
    }

    @Test
    void canManageWeeklySessionGroupParticipants_returnsFalse_forPastSession_nonAdmin() {
        UUID actorUuid = UUID.randomUUID();
        UUID sessionUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        UserSalle actor = new UserSalle();
        actor.setUuid(actorUuid);

        Center center = new Center();
        center.setUuid(centerUuid);

        GroupSalle group = new GroupSalle();
        group.setUuid(groupUuid);
        group.setCenter(center);

        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .group(group)
                .status(1)
                .sessionDateTime(LocalDateTime.now().minusDays(1))
                .title("Sesion")
                .build();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionService.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(authorityService.isOnlyAnimator()).thenReturn(false);
        when(userRepo.findByEmail("leader@example.com")).thenReturn(Optional.of(actor));
        when(userGroupRepo.existsByUser_UuidAndGroup_UuidAndYearAndDeletedAtIsNullAndUserType(
                actorUuid,
                groupUuid,
                2025,
                1)).thenReturn(true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("leader@example.com", "N/A"));

        assertThat(authzBean.canViewWeeklySessionGroupParticipants(sessionUuid, groupUuid)).isTrue();
        assertThat(authzBean.canManageWeeklySessionGroupParticipants(sessionUuid, groupUuid)).isFalse();
    }

    @Test
    void canManageWeeklySessionGroupParticipants_returnsTrue_forAdminOnPastSession() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThat(authzBean.canManageWeeklySessionGroupParticipants(UUID.randomUUID(), UUID.randomUUID())).isTrue();
    }

    @Test
    void canViewWeeklySessionGroupParticipants_returnsFalse_forAnimatorOnDraftSession() {
        UUID actorUuid = UUID.randomUUID();
        UUID sessionUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        UserSalle actor = new UserSalle();
        actor.setUuid(actorUuid);

        Center center = new Center();
        center.setUuid(centerUuid);

        GroupSalle group = new GroupSalle();
        group.setUuid(groupUuid);
        group.setCenter(center);

        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .group(group)
                .status(0)
                .sessionDateTime(LocalDateTime.now())
                .title("Sesion")
                .build();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionService.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(authorityService.isOnlyAnimator()).thenReturn(true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("animator@example.com", "N/A"));

        assertThat(authzBean.canViewWeeklySessionGroupParticipants(sessionUuid, groupUuid)).isFalse();
    }
}
