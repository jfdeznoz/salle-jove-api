package com.sallejoven.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.repository.EventUserRepository;
import com.sallejoven.backend.repository.RefreshTokenRepository;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EventUserService eventUserService;
    @Mock GroupService groupService;
    @Mock AcademicStateService academicStateService;
    @Mock EventService eventService;
    @Mock CenterService centerService;
    @Mock UserCenterService userCenterService;
    @Mock UserRoleHelper roleHelper;
    @Mock UserGroupRepository userGroupRepository;
    @Mock UserCenterRepository userCenterRepository;
    @Mock EventUserRepository eventUserRepository;
    @Mock WeeklySessionUserRepository weeklySessionUserRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks UserService userService;

    @Test
    void searchUsersSmart_usesCenterScope_forDelegatesAndLeaders() {
        UserSalle actor = actor(false);
        UUID centerUuid = UUID.randomUUID();
        UserCenter userCenter = new UserCenter();
        Center center = new Center();
        center.setUuid(centerUuid);
        userCenter.setCenter(center);
        userCenter.setUserType(3);

        List<UserSalle> expected = List.of(actor(false));

        when(userCenterService.findByUserForCurrentYear(actor.getUuid())).thenReturn(List.of(userCenter));
        when(userRepository.searchUsersNormalizedByCenterUuids("maria", java.util.Set.of(centerUuid))).thenReturn(expected);

        assertThat(userService.searchUsersSmart("María", actor)).isEqualTo(expected);

        verify(userRepository).searchUsersNormalizedByCenterUuids("maria", java.util.Set.of(centerUuid));
        verifyNoInteractions(userGroupRepository);
    }

    @Test
    void searchUsersSmart_returnsEmpty_whenActorHasNoCenterScope() {
        UserSalle actor = actor(false);

        when(userCenterService.findByUserForCurrentYear(actor.getUuid())).thenReturn(List.of());

        assertThat(userService.searchUsersSmart("Lucía", actor)).isEmpty();

        verify(userRepository, never()).searchUsersNormalizedByCenterUuids(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anySet());
        verifyNoInteractions(userGroupRepository);
    }

    @Test
    void reactivate_skipsDeletedBaseGroup_whenMergeAlreadyActivatedEquivalentMembership() {
        UserSalle base = deletedUser();
        UserSalle mergeFrom = activeUser();
        GroupSalle group = group();
        UserGroup sourceGroup = UserGroup.builder()
                .uuid(UUID.randomUUID())
                .user(mergeFrom)
                .group(group)
                .userType(0)
                .year(2025)
                .build();
        UserGroup deletedBaseGroup = UserGroup.builder()
                .uuid(UUID.randomUUID())
                .user(base)
                .group(group)
                .userType(0)
                .year(2025)
                .deletedAt(LocalDateTime.now().minusDays(10))
                .build();

        when(userRepository.findByIdForUpdate(base.getUuid())).thenReturn(Optional.of(base));
        when(userRepository.findByIdForUpdate(mergeFrom.getUuid())).thenReturn(Optional.of(mergeFrom));
        when(userGroupRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of(sourceGroup));
        when(userGroupRepository.findActiveByUserGroupYear(base.getUuid(), group.getUuid(), 2025))
                .thenReturn(Optional.empty(), Optional.of(sourceGroup));
        when(userGroupRepository.findDeletedByUser(base.getUuid())).thenReturn(List.of(deletedBaseGroup));
        when(userCenterRepository.findAllByUserIncludingDeleted(base.getUuid())).thenReturn(List.of());
        when(userCenterRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of());
        when(userRepository.save(any(UserSalle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGroupRepository.save(any(UserGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSalle result = userService.reactivate(base.getUuid(), mergeFrom.getUuid());

        assertThat(result.getDeletedAt()).isNull();
        assertThat(sourceGroup.getUser()).isSameAs(base);
        assertThat(sourceGroup.getDeletedAt()).isNull();
        assertThat(deletedBaseGroup.getDeletedAt()).isNotNull();
        assertThat(mergeFrom.getDeletedAt()).isNotNull();

        verify(userGroupRepository).save(sourceGroup);
        verify(userGroupRepository, never()).save(deletedBaseGroup);
        verify(userGroupRepository, never()).reactivateByUser(base.getUuid());
    }

    @Test
    void reactivate_skipsDeletedBaseCenterRole_whenMergeAlreadyActivatedEquivalentCenterRole() {
        UserSalle base = deletedUser();
        UserSalle mergeFrom = activeUser();
        Center center = center();
        UserCenter sourceCenter = UserCenter.builder()
                .uuid(UUID.randomUUID())
                .user(mergeFrom)
                .center(center)
                .userType(3)
                .year(2025)
                .build();
        UserCenter deletedBaseCenter = UserCenter.builder()
                .uuid(UUID.randomUUID())
                .user(base)
                .center(center)
                .userType(3)
                .year(2025)
                .deletedAt(LocalDateTime.now().minusDays(5))
                .build();

        when(userRepository.findByIdForUpdate(base.getUuid())).thenReturn(Optional.of(base));
        when(userRepository.findByIdForUpdate(mergeFrom.getUuid())).thenReturn(Optional.of(mergeFrom));
        when(userGroupRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of());
        when(userGroupRepository.findDeletedByUser(base.getUuid())).thenReturn(List.of());
        when(userCenterRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of(sourceCenter));
        when(userCenterRepository.existsByUser_UuidAndCenter_UuidAndYearAndDeletedAtIsNullAndUserType(
                base.getUuid(), center.getUuid(), 2025, 3
        )).thenReturn(false, true);
        when(userCenterRepository.findAllByUserIncludingDeleted(base.getUuid())).thenReturn(List.of(deletedBaseCenter));
        when(userRepository.save(any(UserSalle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userCenterRepository.save(any(UserCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSalle result = userService.reactivate(base.getUuid(), mergeFrom.getUuid());

        assertThat(result.getDeletedAt()).isNull();
        assertThat(sourceCenter.getUser()).isSameAs(base);
        assertThat(sourceCenter.getDeletedAt()).isNull();
        assertThat(deletedBaseCenter.getDeletedAt()).isNotNull();
        assertThat(mergeFrom.getDeletedAt()).isNotNull();

        verify(userCenterRepository).save(sourceCenter);
        verify(userCenterRepository, never()).save(deletedBaseCenter);
        verify(userCenterRepository, never()).reactivateByUser(base.getUuid());
    }

    @Test
    void reactivate_skipsDeletedBaseEventRegistration_whenMergeAlreadyActivatedEquivalentEventUser() {
        UserSalle base = deletedUser();
        UserSalle mergeFrom = activeUser();
        Event event = event();
        EventUser sourceEventUser = EventUser.builder()
                .uuid(UUID.randomUUID())
                .user(mergeFrom)
                .event(event)
                .status(1)
                .build();
        EventUser deletedBaseEventUser = EventUser.builder()
                .uuid(UUID.randomUUID())
                .user(base)
                .event(event)
                .status(1)
                .deletedAt(LocalDateTime.now().minusDays(3))
                .build();

        when(userRepository.findByIdForUpdate(base.getUuid())).thenReturn(Optional.of(base));
        when(userRepository.findByIdForUpdate(mergeFrom.getUuid())).thenReturn(Optional.of(mergeFrom));
        when(userGroupRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of());
        when(userGroupRepository.findDeletedByUser(base.getUuid())).thenReturn(List.of());
        when(userCenterRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of());
        when(userCenterRepository.findAllByUserIncludingDeleted(base.getUuid())).thenReturn(List.of());
        when(eventUserRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of(sourceEventUser));
        when(eventUserRepository.findAllByUserIncludingDeleted(base.getUuid())).thenReturn(List.of(deletedBaseEventUser));
        when(eventUserRepository.findActiveByEventAndUser(event.getUuid(), base.getUuid()))
                .thenReturn(Optional.empty(), Optional.of(sourceEventUser));
        when(userRepository.save(any(UserSalle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventUserRepository.save(any(EventUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSalle result = userService.reactivate(base.getUuid(), mergeFrom.getUuid());

        assertThat(result.getDeletedAt()).isNull();
        assertThat(sourceEventUser.getUser()).isSameAs(base);
        assertThat(sourceEventUser.getDeletedAt()).isNull();
        assertThat(deletedBaseEventUser.getDeletedAt()).isNotNull();

        verify(eventUserRepository).save(sourceEventUser);
        verify(eventUserRepository, never()).save(deletedBaseEventUser);
        verify(eventUserRepository, never()).reactivateByUser(base.getUuid());
    }

    @Test
    void reactivate_skipsDeletedBaseWeeklySessionAttendance_whenMergeAlreadyActivatedEquivalentSessionUser() {
        UserSalle base = deletedUser();
        UserSalle mergeFrom = activeUser();
        WeeklySession weeklySession = weeklySession();
        WeeklySessionUser sourceSessionUser = WeeklySessionUser.builder()
                .uuid(UUID.randomUUID())
                .user(mergeFrom)
                .weeklySession(weeklySession)
                .status(1)
                .build();
        WeeklySessionUser deletedBaseSessionUser = WeeklySessionUser.builder()
                .uuid(UUID.randomUUID())
                .user(base)
                .weeklySession(weeklySession)
                .status(1)
                .deletedAt(LocalDateTime.now().minusDays(2))
                .build();

        when(userRepository.findByIdForUpdate(base.getUuid())).thenReturn(Optional.of(base));
        when(userRepository.findByIdForUpdate(mergeFrom.getUuid())).thenReturn(Optional.of(mergeFrom));
        when(userGroupRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of());
        when(userGroupRepository.findDeletedByUser(base.getUuid())).thenReturn(List.of());
        when(userCenterRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of());
        when(userCenterRepository.findAllByUserIncludingDeleted(base.getUuid())).thenReturn(List.of());
        when(eventUserRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of());
        when(eventUserRepository.findAllByUserIncludingDeleted(base.getUuid())).thenReturn(List.of());
        when(weeklySessionUserRepository.findAllByUserIncludingDeleted(mergeFrom.getUuid())).thenReturn(List.of(sourceSessionUser));
        when(weeklySessionUserRepository.findAllByUserIncludingDeleted(base.getUuid())).thenReturn(List.of(deletedBaseSessionUser));
        when(weeklySessionUserRepository.findActiveBySessionAndUser(weeklySession.getUuid(), base.getUuid()))
                .thenReturn(Optional.empty(), Optional.of(sourceSessionUser));
        when(userRepository.save(any(UserSalle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(weeklySessionUserRepository.save(any(WeeklySessionUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSalle result = userService.reactivate(base.getUuid(), mergeFrom.getUuid());

        assertThat(result.getDeletedAt()).isNull();
        assertThat(sourceSessionUser.getUser()).isSameAs(base);
        assertThat(sourceSessionUser.getDeletedAt()).isNull();
        assertThat(deletedBaseSessionUser.getDeletedAt()).isNotNull();

        verify(weeklySessionUserRepository).save(sourceSessionUser);
        verify(weeklySessionUserRepository, never()).save(deletedBaseSessionUser);
        verify(weeklySessionUserRepository, never()).reactivateByUser(base.getUuid());
    }

    private static UserSalle actor(boolean admin) {
        UserSalle user = new UserSalle();
        user.setUuid(UUID.randomUUID());
        user.setIsAdmin(admin);
        return user;
    }

    private static UserSalle deletedUser() {
        UserSalle user = actor(false);
        user.setDeletedAt(LocalDateTime.now().minusDays(1));
        return user;
    }

    private static UserSalle activeUser() {
        return actor(false);
    }

    private static GroupSalle group() {
        GroupSalle group = new GroupSalle();
        group.setUuid(UUID.randomUUID());
        return group;
    }

    private static Center center() {
        Center center = new Center();
        center.setUuid(UUID.randomUUID());
        return center;
    }

    private static Event event() {
        Event event = new Event();
        event.setUuid(UUID.randomUUID());
        return event;
    }

    private static WeeklySession weeklySession() {
        WeeklySession weeklySession = new WeeklySession();
        weeklySession.setUuid(UUID.randomUUID());
        return weeklySession;
    }
}
