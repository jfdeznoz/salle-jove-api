package com.sallejoven.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sallejoven.backend.model.dto.UserAttendanceStatsDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.WeeklySessionWarningType;
import com.sallejoven.backend.repository.EventUserRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.WeeklySessionBehaviorWarningRepository;
import com.sallejoven.backend.repository.WeeklySessionRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import com.sallejoven.backend.repository.projection.AttendanceTotalsProjection;
import com.sallejoven.backend.repository.projection.UserLedSessionProjection;
import com.sallejoven.backend.repository.projection.UserRecentSessionProjection;
import com.sallejoven.backend.repository.projection.UserSessionAttendanceStatsProjection;
import com.sallejoven.backend.repository.projection.WarningTotalsProjection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock AcademicStateService academicStateService;
    @Mock UserService userService;
    @Mock GroupService groupService;
    @Mock CenterService centerService;
    @Mock WeeklySessionUserRepository weeklySessionUserRepository;
    @Mock WeeklySessionRepository weeklySessionRepository;
    @Mock WeeklySessionBehaviorWarningRepository weeklySessionBehaviorWarningRepository;
    @Mock EventUserRepository eventUserRepository;
    @Mock UserGroupRepository userGroupRepository;

    @InjectMocks StatsService statsService;

    @Test
    void getUserAttendanceStats_usesParticipantScopedQueriesForResolvedYear() {
        UUID userUuid = UUID.randomUUID();
        UUID sessionUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2025, 10, 5, 18, 30);

        when(userService.findByUserId(userUuid)).thenReturn(UserSalle.builder().uuid(userUuid).build());
        when(weeklySessionUserRepository.findUserAttendanceStats(eq(userUuid), eq(2025), any(), any(), any()))
                .thenReturn(sessionStats(6L, 4L, 1L));
        when(eventUserRepository.findUserAttendanceStats(userUuid, 2025))
                .thenReturn(attendanceTotals(3L, 2L));
        when(weeklySessionBehaviorWarningRepository.findUserWarningTotals(eq(userUuid), any(), any()))
                .thenReturn(warningTotals(1L, 0L));
        when(userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, 2025))
                .thenReturn(List.of(
                        UserGroup.builder()
                                .group(GroupSalle.builder()
                                        .stage(3)
                                        .center(Center.builder().name("Centro A").city("Madrid").build())
                                        .build())
                                .userType(0)
                                .year(2025)
                                .build(),
                        UserGroup.builder()
                                .group(GroupSalle.builder()
                                        .stage(4)
                                        .center(Center.builder().name("Centro B").city("Madrid").build())
                                        .build())
                                .userType(1)
                                .year(2025)
                                .build()));
        when(weeklySessionUserRepository.findRecentSessionsByUser(eq(userUuid), eq(2025), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(recentSession(sessionUuid, now)));
        when(weeklySessionRepository.findLedSessionsByUser(eq(userUuid), eq(2025), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(ledSession(sessionUuid, now)));

        UserAttendanceStatsDto result = statsService.getUserAttendanceStats(userUuid, 2025);

        assertThat(result.sessionAttendance().total()).isEqualTo(6);
        assertThat(result.sessionAttendance().attended()).isEqualTo(4);
        assertThat(result.sessionAttendance().justified()).isEqualTo(1);
        assertThat(result.memberships()).hasSize(2);
        assertThat(result.recentSessions()).hasSize(1);
        assertThat(result.ledSessions()).hasSize(1);

        verify(weeklySessionUserRepository).findUserAttendanceStats(eq(userUuid), eq(2025), any(), any(), any());
        verify(weeklySessionUserRepository).findRecentSessionsByUser(eq(userUuid), eq(2025), any(), any(), any(), any(Pageable.class));
    }

    private UserSessionAttendanceStatsProjection sessionStats(Long total, Long attended, Long justified) {
        return new UserSessionAttendanceStatsProjection() {
            @Override
            public Long getTotal() {
                return total;
            }

            @Override
            public Long getAttended() {
                return attended;
            }

            @Override
            public Long getJustified() {
                return justified;
            }
        };
    }

    private AttendanceTotalsProjection attendanceTotals(Long total, Long attended) {
        return new AttendanceTotalsProjection() {
            @Override
            public Long getTotal() {
                return total;
            }

            @Override
            public Long getAttended() {
                return attended;
            }
        };
    }

    private WarningTotalsProjection warningTotals(Long yellow, Long red) {
        return new WarningTotalsProjection() {
            @Override
            public UUID getReferenceUuid() {
                return null;
            }

            @Override
            public Long getYellowCount() {
                return yellow;
            }

            @Override
            public Long getRedCount() {
                return red;
            }
        };
    }

    private UserRecentSessionProjection recentSession(UUID sessionUuid, LocalDateTime date) {
        return new UserRecentSessionProjection() {
            @Override
            public UUID getSessionUuid() {
                return sessionUuid;
            }

            @Override
            public LocalDateTime getDate() {
                return date;
            }

            @Override
            public String getTitle() {
                return "Sesion";
            }

            @Override
            public String getVitalSituationTitle() {
                return "Comunidad";
            }

            @Override
            public String getVitalSituationSessionTitle() {
                return "Tema";
            }

            @Override
            public Boolean getAttended() {
                return true;
            }

            @Override
            public Boolean getJustified() {
                return false;
            }

            @Override
            public WeeklySessionWarningType getWarningType() {
                return null;
            }
        };
    }

    private UserLedSessionProjection ledSession(UUID sessionUuid, LocalDateTime date) {
        return new UserLedSessionProjection() {
            @Override
            public UUID getSessionUuid() {
                return sessionUuid;
            }

            @Override
            public LocalDateTime getDate() {
                return date;
            }

            @Override
            public String getTitle() {
                return "Sesion dirigida";
            }

            @Override
            public String getVitalSituationTitle() {
                return "Comunidad";
            }

            @Override
            public String getVitalSituationSessionTitle() {
                return "Tema";
            }

            @Override
            public String getContent() {
                return "Contenido";
            }

            @Override
            public String getObservations() {
                return "Observaciones";
            }

            @Override
            public String getCenterName() {
                return "Centro B";
            }

            @Override
            public Integer getStage() {
                return 4;
            }
        };
    }
}
