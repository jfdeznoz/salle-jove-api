package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.WeeklySessionBehaviorWarningRepository;
import com.sallejoven.backend.repository.WeeklySessionRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklySessionUserServiceTest {

    @Mock WeeklySessionUserRepository weeklySessionUserRepository;
    @Mock WeeklySessionRepository weeklySessionRepository;
    @Mock AcademicStateService academicStateService;
    @Mock UserGroupRepository userGroupRepository;
    @Mock WeeklySessionBehaviorWarningRepository weeklySessionBehaviorWarningRepository;
    @Mock AuthService authService;
    @Mock ObservationNotificationService observationNotificationService;

    @InjectMocks WeeklySessionUserService weeklySessionUserService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(authService.getCurrentUser()).thenReturn(user(UUID.randomUUID()));
        lenient().when(weeklySessionBehaviorWarningRepository.findByWeeklySessionUserUuidIncludingDeleted(any()))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateParticipantsAttendance_throwsSessionLocked_forPastSession_nonAdmin() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .status(1)
                .title("Sesion")
                .sessionDateTime(LocalDateTime.now().minusDays(1))
                .build();
        WeeklySessionUser sessionUser = WeeklySessionUser.builder()
                .uuid(UUID.randomUUID())
                .weeklySession(session)
                .user(user(userUuid))
                .status(null)
                .build();

        when(weeklySessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(userGroupRepository.findActiveByUserGroupYear(userUuid, groupUuid, 2025))
                .thenReturn(Optional.of(UserGroup.builder().user(user(userUuid)).userType(0).year(2025).build()));
        when(weeklySessionUserRepository.findBySessionUuidAndUserUuid(sessionUuid, userUuid))
                .thenReturn(Optional.of(sessionUser));

        AttendanceUpdateDto update = attendanceUpdate(userUuid, 0);

        assertThatThrownBy(() -> weeklySessionUserService.updateParticipantsAttendance(
                sessionUuid,
                List.of(update),
                groupUuid))
                .isInstanceOfSatisfying(SalleException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCodes.SESSION_LOCKED.getErrorCode()));

        verify(weeklySessionUserRepository).findBySessionUuidAndUserUuid(sessionUuid, userUuid);
        verifyNoMoreInteractions(weeklySessionUserRepository);
    }

    @Test
    void updateParticipantsAttendance_allowsPastSession_forAdmin() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .status(1)
                .title("Sesion")
                .sessionDateTime(LocalDateTime.now().minusDays(1))
                .build();
        WeeklySessionUser sessionUser = WeeklySessionUser.builder()
                .uuid(UUID.randomUUID())
                .weeklySession(session)
                .user(user(userUuid))
                .status(null)
                .build();

        when(weeklySessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(userGroupRepository.findActiveByUserGroupYear(userUuid, groupUuid, 2025))
                .thenReturn(Optional.of(UserGroup.builder().user(user(userUuid)).userType(0).year(2025).build()));
        when(weeklySessionUserRepository.findBySessionUuidAndUserUuid(sessionUuid, userUuid))
                .thenReturn(Optional.of(sessionUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        weeklySessionUserService.updateParticipantsAttendance(
                sessionUuid,
                List.of(attendanceUpdate(userUuid, 1)),
                groupUuid);

        assertThat(sessionUser.getStatus()).isEqualTo(1);
        verify(weeklySessionUserRepository).save(sessionUser);
    }

    @Test
    void findBySessionIdAndGroupId_returnsOnlyParticipants() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID participantUuid = UUID.randomUUID();
        UUID animatorUuid = UUID.randomUUID();

        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .group(com.sallejoven.backend.model.entity.GroupSalle.builder().uuid(groupUuid).build())
                .status(1)
                .title("Sesion")
                .sessionDateTime(LocalDateTime.now())
                .build();

        WeeklySessionUser participant = WeeklySessionUser.builder()
                .uuid(UUID.randomUUID())
                .weeklySession(session)
                .user(user(participantUuid))
                .status(null)
                .build();
        WeeklySessionUser animator = WeeklySessionUser.builder()
                .uuid(UUID.randomUUID())
                .weeklySession(session)
                .user(user(animatorUuid))
                .status(null)
                .build();

        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(weeklySessionUserRepository.findBySessionUuidOrdered(sessionUuid))
                .thenReturn(List.of(participant, animator));
        when(userGroupRepository.findByGroup_UuidAndYearAndDeletedAtIsNullAndUser_UuidIn(
                groupUuid,
                2025,
                List.of(participantUuid, animatorUuid)))
                .thenReturn(List.of(
                        UserGroup.builder().user(user(participantUuid)).userType(0).year(2025).build(),
                        UserGroup.builder().user(user(animatorUuid)).userType(1).year(2025).build()
                ));

        List<WeeklySessionUser> result = weeklySessionUserService.findBySessionIdAndGroupId(sessionUuid, groupUuid);

        assertThat(result).extracting(item -> item.getUser().getUuid())
                .containsExactly(participantUuid);
    }

    @Test
    void updateParticipantsAttendance_throwsSessionLocked_forArchivedSession_evenForAdmin() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        when(weeklySessionRepository.findById(sessionUuid)).thenReturn(Optional.of(
                WeeklySession.builder()
                        .uuid(sessionUuid)
                        .status(2)
                        .title("Sesion")
                        .sessionDateTime(LocalDateTime.now())
                        .build()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThatThrownBy(() -> weeklySessionUserService.updateParticipantsAttendance(
                sessionUuid,
                List.of(attendanceUpdate(UUID.randomUUID(), 0)),
                groupUuid))
                .isInstanceOfSatisfying(SalleException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCodes.SESSION_LOCKED.getErrorCode()));

        verifyNoInteractions(weeklySessionUserRepository);
    }

    @Test
    void updateParticipantsAttendance_allowsResetToPending_forAdmin() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .status(1)
                .title("Sesion")
                .sessionDateTime(LocalDateTime.now().minusDays(1))
                .build();
        WeeklySessionUser sessionUser = WeeklySessionUser.builder()
                .uuid(UUID.randomUUID())
                .weeklySession(session)
                .user(user(userUuid))
                .status(1)
                .justified(true)
                .justificationReason("Avisada")
                .build();

        when(weeklySessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(userGroupRepository.findActiveByUserGroupYear(userUuid, groupUuid, 2025))
                .thenReturn(Optional.of(UserGroup.builder().user(user(userUuid)).userType(0).year(2025).build()));
        when(weeklySessionUserRepository.findBySessionUuidAndUserUuid(sessionUuid, userUuid))
                .thenReturn(Optional.of(sessionUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        weeklySessionUserService.updateParticipantsAttendance(
                sessionUuid,
                List.of(attendanceUpdate(userUuid, null)),
                groupUuid);

        assertThat(sessionUser.getStatus()).isNull();
        assertThat(sessionUser.getJustified()).isFalse();
        assertThat(sessionUser.getJustificationReason()).isNull();
        verify(weeklySessionUserRepository).save(sessionUser);
    }

    @Test
    void updateParticipantsAttendance_marksJustifiedAbsence_whenRequested() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .status(1)
                .title("Sesion")
                .sessionDateTime(LocalDateTime.now().minusDays(1))
                .build();
        WeeklySessionUser sessionUser = WeeklySessionUser.builder()
                .uuid(UUID.randomUUID())
                .weeklySession(session)
                .user(user(userUuid))
                .status(null)
                .build();

        when(weeklySessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(userGroupRepository.findActiveByUserGroupYear(userUuid, groupUuid, 2025))
                .thenReturn(Optional.of(UserGroup.builder().user(user(userUuid)).userType(0).year(2025).build()));
        when(weeklySessionUserRepository.findBySessionUuidAndUserUuid(sessionUuid, userUuid))
                .thenReturn(Optional.of(sessionUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        weeklySessionUserService.updateParticipantsAttendance(
                sessionUuid,
                List.of(justifiedAttendanceUpdate(userUuid, "Familia")),
                groupUuid);

        assertThat(sessionUser.getStatus()).isEqualTo(0);
        assertThat(sessionUser.getJustified()).isTrue();
        assertThat(sessionUser.getJustificationReason()).isEqualTo("Familia");
        verify(weeklySessionUserRepository).save(sessionUser);
    }

    @Test
    void updateParticipantsAttendance_allowsJustifyingExistingAbsence_forPastSession_nonAdmin() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .status(1)
                .title("Sesion")
                .sessionDateTime(LocalDateTime.now().minusDays(1))
                .build();
        WeeklySessionUser sessionUser = WeeklySessionUser.builder()
                .uuid(UUID.randomUUID())
                .weeklySession(session)
                .user(user(userUuid))
                .status(0)
                .justified(false)
                .build();

        when(weeklySessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(userGroupRepository.findActiveByUserGroupYear(userUuid, groupUuid, 2025))
                .thenReturn(Optional.of(UserGroup.builder().user(user(userUuid)).userType(0).year(2025).build()));
        when(weeklySessionUserRepository.findBySessionUuidAndUserUuid(sessionUuid, userUuid))
                .thenReturn(Optional.of(sessionUser));

        weeklySessionUserService.updateParticipantsAttendance(
                sessionUuid,
                List.of(justifiedAttendanceUpdate(userUuid, "Familia")),
                groupUuid);

        assertThat(sessionUser.getStatus()).isEqualTo(0);
        assertThat(sessionUser.getJustified()).isTrue();
        assertThat(sessionUser.getJustificationReason()).isEqualTo("Familia");
        verify(weeklySessionUserRepository).save(sessionUser);
    }

    @Test
    void assignSessionToUserGroup_startsAsPending() {
        WeeklySession session = WeeklySession.builder()
                .uuid(UUID.randomUUID())
                .build();
        UserGroup userGroup = UserGroup.builder()
                .user(user(UUID.randomUUID()))
                .userType(0)
                .year(2025)
                .build();

        weeklySessionUserService.assignSessionToUserGroup(session, userGroup);

        verify(weeklySessionUserRepository).save(org.mockito.ArgumentMatchers.argThat(
                sessionUser -> sessionUser.getStatus() == null
                        && session.equals(sessionUser.getWeeklySession())
                        && userGroup.getUser().equals(sessionUser.getUser())
        ));
    }

    private static AttendanceUpdateDto attendanceUpdate(UUID userUuid, Integer attends) {
        AttendanceUpdateDto dto = new AttendanceUpdateDto();
        dto.setUserUuid(userUuid.toString());
        dto.setAttends(attends);
        return dto;
    }

    private static AttendanceUpdateDto justifiedAttendanceUpdate(UUID userUuid, String reason) {
        AttendanceUpdateDto dto = attendanceUpdate(userUuid, 0);
        dto.setJustified(true);
        dto.setJustificationReason(reason);
        return dto;
    }

    private static UserSalle user(UUID uuid) {
        UserSalle user = new UserSalle();
        user.setUuid(uuid);
        return user;
    }
}
