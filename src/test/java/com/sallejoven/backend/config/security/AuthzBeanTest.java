package com.sallejoven.backend.config.security;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.WeeklySessionEditRequest;
import java.time.LocalDateTime;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.repository.EventGroupRepository;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserPendingRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.repository.WeeklySessionRepository;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.AuthorityService;
import com.sallejoven.backend.service.EventService;
import com.sallejoven.backend.service.VitalSituationService;
import com.sallejoven.backend.service.WeeklySessionService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.ZoneId;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthzBeanTest {

    private static final ZoneId MADRID_ZONE = ZoneId.of("Europe/Madrid");

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
    @Mock WeeklySessionRepository weeklySessionRepository;
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

        UserSalle actor = new UserSalle();
        actor.setUuid(actorUuid);

        UserSalle target = new UserSalle();
        target.setUuid(targetUuid);

        when(userRepo.findByUuid(targetUuid)).thenReturn(Optional.of(target));
        when(userRepo.findByEmail("leader@example.com")).thenReturn(Optional.of(actor));
        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(userGroupRepo.findDistinctCenterUuidsByUserUuidAndYear(targetUuid, 2025)).thenReturn(List.of(centerUuid));
        when(userCenterRepo.findDistinctCenterUuidsByUserUuidAndYear(targetUuid, 2025)).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "leader@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("CENTER:" + centerUuid + ":GROUP_LEADER:2025"))));

        boolean allowed = authzBean.canViewUserStats(targetUuid.toString());

        assertThat(allowed).isTrue();
        verify(userRepo).findByUuid(targetUuid);
        verify(userRepo).findByEmail("leader@example.com");
        verify(academicStateService).getVisibleYearOrNull();
        verify(userGroupRepo).findDistinctCenterUuidsByUserUuidAndYear(targetUuid, 2025);
        verify(userCenterRepo).findDistinctCenterUuidsByUserUuidAndYear(targetUuid, 2025);
        verifyNoMoreInteractions(userRepo, userGroupRepo, userCenterRepo, academicStateService, authorityService);
    }

    @Test
    void canManageUser_returnsTrue_forAdmin() {
        UUID targetUuid = UUID.randomUUID();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThat(authzBean.canManageUser(targetUuid)).isTrue();
        verifyNoMoreInteractions(userRepo, userGroupRepo, userCenterRepo, academicStateService, authorityService);
    }

    @Test
    void canViewUserGroups_returnsTrue_forAnimatorViewingSharedCatechumen() {
        UUID actorUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        UUID sharedGroupUuid = UUID.randomUUID();

        UserSalle actor = new UserSalle();
        actor.setUuid(actorUuid);

        UserSalle target = new UserSalle();
        target.setUuid(targetUuid);

        GroupSalle sharedGroup = new GroupSalle();
        sharedGroup.setUuid(sharedGroupUuid);

        var participantMembership = mock(com.sallejoven.backend.model.entity.UserGroup.class);
        when(participantMembership.getGroup()).thenReturn(sharedGroup);

        when(userRepo.findByUuid(targetUuid)).thenReturn(Optional.of(target));
        when(userRepo.findByEmail("animator@example.com")).thenReturn(Optional.of(actor));
        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(authorityService.extractAnimatorGroupIdsForYear(
                Set.of("GROUP:" + sharedGroupUuid + ":ANIMATOR:2025"), 2025))
                .thenReturn(Set.of(sharedGroupUuid));
        when(userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNullAndUserType(targetUuid, 2025, 0))
                .thenReturn(List.of(participantMembership));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + sharedGroupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canViewUserGroups(targetUuid)).isTrue();
        assertThat(authzBean.canViewUserCenters(targetUuid)).isTrue();
        assertThat(authzBean.canViewUserStats(targetUuid.toString())).isTrue();
    }

    @Test
    void canViewUserGroups_returnsFalse_forAnimatorViewingCatechumenFromOtherGroup() {
        UUID actorUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        UUID actorGroupUuid = UUID.randomUUID();
        UUID targetGroupUuid = UUID.randomUUID();

        UserSalle actor = new UserSalle();
        actor.setUuid(actorUuid);

        UserSalle target = new UserSalle();
        target.setUuid(targetUuid);

        GroupSalle targetGroup = new GroupSalle();
        targetGroup.setUuid(targetGroupUuid);

        var participantMembership = mock(com.sallejoven.backend.model.entity.UserGroup.class);
        when(participantMembership.getGroup()).thenReturn(targetGroup);

        when(userRepo.findByUuid(targetUuid)).thenReturn(Optional.of(target));
        when(userRepo.findByEmail("animator@example.com")).thenReturn(Optional.of(actor));
        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(authorityService.extractAnimatorGroupIdsForYear(
                Set.of("GROUP:" + actorGroupUuid + ":ANIMATOR:2025"), 2025))
                .thenReturn(Set.of(actorGroupUuid));
        when(userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNullAndUserType(targetUuid, 2025, 0))
                .thenReturn(List.of(participantMembership));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + actorGroupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canViewUserGroups(targetUuid)).isFalse();
        assertThat(authzBean.canViewUserCenters(targetUuid)).isFalse();
        assertThat(authzBean.canViewUserStats(targetUuid.toString())).isFalse();
    }

    @Test
    void canViewUserGroups_returnsFalse_forAnimatorViewingUserWithoutParticipantMembership() {
        UUID actorUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        UUID sharedGroupUuid = UUID.randomUUID();

        UserSalle actor = new UserSalle();
        actor.setUuid(actorUuid);

        UserSalle target = new UserSalle();
        target.setUuid(targetUuid);

        when(userRepo.findByUuid(targetUuid)).thenReturn(Optional.of(target));
        when(userRepo.findByEmail("animator@example.com")).thenReturn(Optional.of(actor));
        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(authorityService.extractAnimatorGroupIdsForYear(
                Set.of("GROUP:" + sharedGroupUuid + ":ANIMATOR:2025"), 2025))
                .thenReturn(Set.of(sharedGroupUuid));
        when(userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNullAndUserType(targetUuid, 2025, 0))
                .thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + sharedGroupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canViewUserGroups(targetUuid)).isFalse();
        assertThat(authzBean.canViewUserStats(targetUuid.toString())).isFalse();
    }

    @Test
    void canManageWeeklySessionForEditOrDelete_returnsTrue_forAnimatorWithFutureSession() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();

        WeeklySessionRepository.AuthView authView = new WeeklySessionRepository.AuthView() {
            @Override
            public UUID getUuid() {
                return sessionUuid;
            }

            @Override
            public Integer getStatus() {
                return 1;
            }

            @Override
            public UUID getGroupUuid() {
                return groupUuid;
            }

            @Override
            public UUID getCenterUuid() {
                return centerUuid;
            }

            @Override
            public LocalDateTime getSessionDateTime() {
                return LocalDateTime.now(MADRID_ZONE).plusHours(2);
            }
        };

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionRepository.findAuthViewByUuid(sessionUuid)).thenReturn(Optional.of(authView));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canManageWeeklySessionForEditOrDelete(sessionUuid)).isTrue();
    }

    @Test
    void canManageWeeklySessionForEditOrDelete_returnsFalse_forAnimatorWithPastSession() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();

        WeeklySessionRepository.AuthView authView = new WeeklySessionRepository.AuthView() {
            @Override
            public UUID getUuid() {
                return sessionUuid;
            }

            @Override
            public Integer getStatus() {
                return 1;
            }

            @Override
            public UUID getGroupUuid() {
                return groupUuid;
            }

            @Override
            public UUID getCenterUuid() {
                return centerUuid;
            }

            @Override
            public LocalDateTime getSessionDateTime() {
                return LocalDateTime.now(MADRID_ZONE).minusHours(2);
            }
        };

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionRepository.findAuthViewByUuid(sessionUuid)).thenReturn(Optional.of(authView));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canManageWeeklySessionForEditOrDelete(sessionUuid)).isFalse();
    }

    @Test
    void canEditWeeklySession_returnsTrue_forAnimatorObservationsOnSessionDay() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();

        WeeklySessionRepository.AuthView authView = new WeeklySessionRepository.AuthView() {
            @Override
            public UUID getUuid() {
                return sessionUuid;
            }

            @Override
            public Integer getStatus() {
                return 1;
            }

            @Override
            public UUID getGroupUuid() {
                return groupUuid;
            }

            @Override
            public UUID getCenterUuid() {
                return centerUuid;
            }

            @Override
            public LocalDateTime getSessionDateTime() {
                return LocalDateTime.now(MADRID_ZONE).withHour(18);
            }
        };

        WeeklySessionEditRequest request = WeeklySessionEditRequest.builder()
                .observations("Nueva observacion")
                .build();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionRepository.findAuthViewByUuid(sessionUuid)).thenReturn(Optional.of(authView));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canEditWeeklySession(sessionUuid, request)).isTrue();
        assertThat(authzBean.canEditWeeklySessionObservations(sessionUuid)).isTrue();
    }

    @Test
    void canEditWeeklySession_returnsFalse_forAnimatorObservationsAfterSessionDay() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();

        WeeklySessionRepository.AuthView authView = new WeeklySessionRepository.AuthView() {
            @Override
            public UUID getUuid() {
                return sessionUuid;
            }

            @Override
            public Integer getStatus() {
                return 1;
            }

            @Override
            public UUID getGroupUuid() {
                return groupUuid;
            }

            @Override
            public UUID getCenterUuid() {
                return centerUuid;
            }

            @Override
            public LocalDateTime getSessionDateTime() {
                return LocalDateTime.now(MADRID_ZONE).minusDays(1);
            }
        };

        WeeklySessionEditRequest request = WeeklySessionEditRequest.builder()
                .observations("Nueva observacion")
                .build();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionRepository.findAuthViewByUuid(sessionUuid)).thenReturn(Optional.of(authView));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canEditWeeklySession(sessionUuid, request)).isFalse();
        assertThat(authzBean.canEditWeeklySessionObservations(sessionUuid)).isFalse();
    }

    @Test
    void canEditWeeklySession_returnsFalse_forAnimatorTryingToEditTitleOnSessionDay() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();

        WeeklySessionRepository.AuthView authView = new WeeklySessionRepository.AuthView() {
            @Override
            public UUID getUuid() {
                return sessionUuid;
            }

            @Override
            public Integer getStatus() {
                return 1;
            }

            @Override
            public UUID getGroupUuid() {
                return groupUuid;
            }

            @Override
            public UUID getCenterUuid() {
                return centerUuid;
            }

            @Override
            public LocalDateTime getSessionDateTime() {
                return LocalDateTime.now(MADRID_ZONE).withHour(18);
            }
        };

        WeeklySessionEditRequest request = WeeklySessionEditRequest.builder()
                .title("Titulo editado")
                .observations("Nueva observacion")
                .build();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionRepository.findAuthViewByUuid(sessionUuid)).thenReturn(Optional.of(authView));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canEditWeeklySession(sessionUuid, request)).isFalse();
    }

    @Test
    void canEditWeeklySession_returnsFalse_forParticipantObservationsOnSessionDay() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();

        WeeklySessionRepository.AuthView authView = new WeeklySessionRepository.AuthView() {
            @Override
            public UUID getUuid() {
                return sessionUuid;
            }

            @Override
            public Integer getStatus() {
                return 1;
            }

            @Override
            public UUID getGroupUuid() {
                return groupUuid;
            }

            @Override
            public UUID getCenterUuid() {
                return centerUuid;
            }

            @Override
            public LocalDateTime getSessionDateTime() {
                return LocalDateTime.now(MADRID_ZONE).withHour(18);
            }
        };

        WeeklySessionEditRequest request = WeeklySessionEditRequest.builder()
                .observations("Nueva observacion")
                .build();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionRepository.findAuthViewByUuid(sessionUuid)).thenReturn(Optional.of(authView));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("participant@example.com", "N/A"));

        assertThat(authzBean.canEditWeeklySession(sessionUuid, request)).isFalse();
        assertThat(authzBean.canEditWeeklySessionObservations(sessionUuid)).isFalse();
    }

    @Test
    void canEditWeeklySession_returnsTrue_forGroupLeaderEditingTitleWithoutObservations() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();

        WeeklySessionRepository.AuthView authView = new WeeklySessionRepository.AuthView() {
            @Override
            public UUID getUuid() {
                return sessionUuid;
            }

            @Override
            public Integer getStatus() {
                return 1;
            }

            @Override
            public UUID getGroupUuid() {
                return groupUuid;
            }

            @Override
            public UUID getCenterUuid() {
                return centerUuid;
            }

            @Override
            public LocalDateTime getSessionDateTime() {
                return LocalDateTime.now(MADRID_ZONE).minusDays(2);
            }
        };

        WeeklySessionEditRequest request = WeeklySessionEditRequest.builder()
                .title("Titulo editado")
                .build();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionRepository.findAuthViewByUuid(sessionUuid)).thenReturn(Optional.of(authView));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "leader@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("CENTER:" + centerUuid + ":GROUP_LEADER:2025"))));

        assertThat(authzBean.canEditWeeklySession(sessionUuid, request)).isTrue();
    }

    @Test
    void canEditUserAsAnimator_returnsTrue_forSharedCatechumen() {
        UUID targetUuid = UUID.randomUUID();
        UUID sharedGroupUuid = UUID.randomUUID();

        GroupSalle sharedGroup = new GroupSalle();
        sharedGroup.setUuid(sharedGroupUuid);

        var participantMembership = mock(com.sallejoven.backend.model.entity.UserGroup.class);
        when(participantMembership.getGroup()).thenReturn(sharedGroup);

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(authorityService.extractAnimatorGroupIdsForYear(
                Set.of("GROUP:" + sharedGroupUuid + ":ANIMATOR:2025"), 2025))
                .thenReturn(Set.of(sharedGroupUuid));
        when(userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNullAndUserType(targetUuid, 2025, 0))
                .thenReturn(List.of(participantMembership));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + sharedGroupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canEditUserAsAnimator(targetUuid)).isTrue();
    }

    @Test
    void canEditUserAsAnimator_returnsFalse_forSharedCatechist() {
        UUID targetUuid = UUID.randomUUID();
        UUID sharedGroupUuid = UUID.randomUUID();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(authorityService.extractAnimatorGroupIdsForYear(
                Set.of("GROUP:" + sharedGroupUuid + ":ANIMATOR:2025"), 2025))
                .thenReturn(Set.of(sharedGroupUuid));
        when(userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNullAndUserType(targetUuid, 2025, 0))
                .thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + sharedGroupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canEditUserAsAnimator(targetUuid)).isFalse();
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
        UUID sessionUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

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

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

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
    void canUpdateWeeklySessionGroupParticipants_returnsTrue_forPastSession_nonAdmin() {
        UUID sessionUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

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

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canUpdateWeeklySessionGroupParticipants(sessionUuid, groupUuid)).isTrue();
    }

    @Test
    void canManageEventGroupParticipants_returnsFalse_forAnimator() {
        UUID eventUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        Center center = new Center();
        center.setUuid(centerUuid);

        GroupSalle group = new GroupSalle();
        group.setUuid(groupUuid);
        group.setCenter(center);

        EventGroup eventGroup = EventGroup.builder()
                .groupSalle(group)
                .build();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(eventGroupRepo.findByEventUuid(eventUuid)).thenReturn(List.of(eventGroup));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canManageEventGroupParticipants(eventUuid, groupUuid)).isFalse();
    }

    @Test
    void canSearchUsers_returnsFalse_forAnimator() {
        UUID groupUuid = UUID.randomUUID();
        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(authorityService.extractCenterIdsForYear(Set.of("GROUP:" + groupUuid + ":ANIMATOR:2025"), 2025))
                .thenReturn(Set.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canSearchUsers()).isFalse();
    }

    @Test
    void canViewUserGroups_returnsFalse_forAnimatorWithoutParticipantMembership() {
        UUID actorUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        UserSalle actor = new UserSalle();
        actor.setUuid(actorUuid);

        when(userRepo.findByEmail("animator@example.com")).thenReturn(Optional.of(actor));
        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(authorityService.extractAnimatorGroupIdsForYear(
                Set.of("GROUP:" + groupUuid + ":ANIMATOR:2025"), 2025))
                .thenReturn(Set.of(groupUuid));
        when(userGroupRepo.findByUser_UuidAndYearAndDeletedAtIsNullAndUserType(targetUuid, 2025, 0))
                .thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canViewUserGroups(targetUuid)).isFalse();
    }

    @Test
    void canViewWeeklySessionGroupParticipants_returnsTrue_forAnimatorOnDraftSession() {
        UUID sessionUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

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

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canViewWeeklySessionGroupParticipants(sessionUuid, groupUuid)).isTrue();
    }

    @Test
    void canViewWeeklySessionGroupParticipants_resolvesCenterWithoutTouchingDetachedLazyGroup() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID centerUuid = UUID.randomUUID();

        Center center = new Center();
        center.setUuid(centerUuid);

        GroupSalle persistedGroup = new GroupSalle();
        persistedGroup.setUuid(groupUuid);
        persistedGroup.setCenter(center);

        GroupSalle detachedLazyGroup = mock(GroupSalle.class);
        when(detachedLazyGroup.getUuid()).thenReturn(groupUuid);

        WeeklySession session = mock(WeeklySession.class);
        when(session.getGroup()).thenReturn(detachedLazyGroup);

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(weeklySessionService.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(groupRepository.findById(groupUuid)).thenReturn(Optional.of(persistedGroup));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "leader@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("CENTER:" + centerUuid + ":GROUP_LEADER:2025"))));

        assertThat(authzBean.canViewWeeklySessionGroupParticipants(sessionUuid, groupUuid)).isTrue();
    }

    @Test
    void canViewVitalSituations_returnsTrue_forAnimatorInGlobalMode() {
        UUID groupUuid = UUID.randomUUID();

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(authorityService.extractCenterIdsForYear(Set.of("GROUP:" + groupUuid + ":ANIMATOR:2025"), 2025))
                .thenReturn(Set.of());
        when(authorityService.extractAnimatorGroupIdsForYear(Set.of("GROUP:" + groupUuid + ":ANIMATOR:2025"), 2025))
                .thenReturn(Set.of(groupUuid));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "animator@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("GROUP:" + groupUuid + ":ANIMATOR:2025"))));

        assertThat(authzBean.canViewVitalSituations(null)).isTrue();
    }

    @Test
    void canViewVitalSituations_returnsTrue_forGroupLeaderOnGroupScope() {
        UUID centerUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        Center center = new Center();
        center.setUuid(centerUuid);

        GroupSalle group = new GroupSalle();
        group.setUuid(groupUuid);
        group.setCenter(center);

        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(groupRepository.findById(groupUuid)).thenReturn(Optional.of(group));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "leader@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("CENTER:" + centerUuid + ":GROUP_LEADER:2025"))));

        assertThat(authzBean.canViewVitalSituations(groupUuid.toString())).isTrue();
    }

    @Test
    void canViewVitalSituations_returnsFalse_forParticipant() {
        when(academicStateService.getVisibleYearOrNull()).thenReturn(2025);
        when(authorityService.extractCenterIdsForYear(Set.of(), 2025)).thenReturn(Set.of());
        when(authorityService.extractAnimatorGroupIdsForYear(Set.of(), 2025)).thenReturn(Set.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("participant@example.com", "N/A"));

        assertThat(authzBean.canViewVitalSituations(null)).isFalse();
        assertThat(authzBean.canViewVitalSituation(UUID.randomUUID().toString())).isFalse();
        assertThat(authzBean.canViewVitalSituationSession(UUID.randomUUID().toString())).isFalse();
    }
}
