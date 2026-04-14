package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.repository.UserGroupRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklySessionUserServiceTest {

    @Mock WeeklySessionUserRepository weeklySessionUserRepository;
    @Mock WeeklySessionRepository weeklySessionRepository;
    @Mock AcademicStateService academicStateService;
    @Mock UserGroupRepository userGroupRepository;

    @InjectMocks WeeklySessionUserService weeklySessionUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateParticipantsAttendance_throwsSessionLocked_forPastSession_nonAdmin() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        when(weeklySessionRepository.findById(sessionUuid)).thenReturn(Optional.of(
                WeeklySession.builder()
                        .uuid(sessionUuid)
                        .status(1)
                        .title("Sesion")
                        .sessionDateTime(LocalDateTime.now().minusDays(1))
                        .build()));

        AttendanceUpdateDto update = attendanceUpdate(UUID.randomUUID(), 0);

        assertThatThrownBy(() -> weeklySessionUserService.updateParticipantsAttendance(
                sessionUuid,
                List.of(update),
                groupUuid))
                .isInstanceOfSatisfying(SalleException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCodes.SESSION_LOCKED.getErrorCode()));

        verifyNoInteractions(weeklySessionUserRepository);
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
                .status(0)
                .build();

        when(weeklySessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(weeklySessionUserRepository.findBySessionUserAndGroup(sessionUuid, userUuid, groupUuid, 2025))
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

    private static AttendanceUpdateDto attendanceUpdate(UUID userUuid, int attends) {
        AttendanceUpdateDto dto = new AttendanceUpdateDto();
        dto.setUserUuid(userUuid.toString());
        dto.setAttends(attends);
        return dto;
    }

    private static UserSalle user(UUID uuid) {
        UserSalle user = new UserSalle();
        user.setUuid(uuid);
        return user;
    }
}
