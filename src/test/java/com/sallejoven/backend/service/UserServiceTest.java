package com.sallejoven.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.repository.EventUserRepository;
import com.sallejoven.backend.repository.RefreshTokenRepository;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import java.util.List;
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

    private static UserSalle actor(boolean admin) {
        UserSalle user = new UserSalle();
        user.setUuid(UUID.randomUUID());
        user.setIsAdmin(admin);
        return user;
    }
}
