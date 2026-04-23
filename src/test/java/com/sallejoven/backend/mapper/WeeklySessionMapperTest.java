package com.sallejoven.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sallejoven.backend.model.dto.WeeklySessionDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.VitalSituation;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.repository.WeeklySessionBehaviorWarningRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.AuthService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WeeklySessionMapperTest {

    @Mock WeeklySessionUserRepository weeklySessionUserRepository;
    @Mock AcademicStateService academicStateService;
    @Mock WeeklySessionBehaviorWarningRepository weeklySessionBehaviorWarningRepository;
    @Mock AuthService authService;

    private WeeklySessionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WeeklySessionMapperImpl();
        ReflectionTestUtils.setField(mapper, "weeklySessionUserRepository", weeklySessionUserRepository);
        ReflectionTestUtils.setField(mapper, "academicStateService", academicStateService);
        ReflectionTestUtils.setField(mapper, "weeklySessionBehaviorWarningRepository", weeklySessionBehaviorWarningRepository);
        ReflectionTestUtils.setField(mapper, "authService", authService);
    }

    @Test
    void toDto_doesNotExposePersonalAttendanceForCatechistOnlyMembership() {
        UUID sessionUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .title("Sesion semanal")
                .group(GroupSalle.builder()
                        .uuid(groupUuid)
                        .stage(4)
                        .center(Center.builder().uuid(UUID.randomUUID()).name("Centro").city("Madrid").build())
                        .build())
                .vitalSituationSession(VitalSituationSession.builder()
                        .uuid(UUID.randomUUID())
                        .title("Tema")
                        .vitalSituation(VitalSituation.builder()
                                .uuid(UUID.randomUUID())
                                .title("Comunidad")
                                .stages(new Integer[] {4})
                                .build())
                        .build())
                .sessionDateTime(LocalDateTime.of(2025, 10, 5, 18, 30))
                .status(1)
                .build();
        UserSalle currentUser = UserSalle.builder().uuid(userUuid).build();

        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(weeklySessionUserRepository.countAttendanceBySessionUuid(sessionUuid, 2025))
                .thenReturn(counts(3L, 5L));
        when(weeklySessionBehaviorWarningRepository.findSessionWarningTotals(sessionUuid))
                .thenReturn(warningTotals(0L, 0L));
        when(weeklySessionUserRepository.findBySessionUserAndGroup(sessionUuid, userUuid, groupUuid, 2025))
                .thenReturn(Optional.empty());

        WeeklySessionDto dto = mapper.toDto(session);

        assertThat(dto.currentUserAttendanceStatus()).isNull();
        assertThat(dto.currentUserJustified()).isFalse();
        verify(weeklySessionUserRepository).findBySessionUserAndGroup(sessionUuid, userUuid, groupUuid, 2025);
        verify(weeklySessionUserRepository, never()).findBySessionUuidAndUserUuid(sessionUuid, userUuid);
    }

    private WeeklySessionUserRepository.AttendanceCountProjection counts(Long attendanceCount, Long totalCount) {
        return new WeeklySessionUserRepository.AttendanceCountProjection() {
            @Override
            public Long getAttendanceCount() {
                return attendanceCount;
            }

            @Override
            public Long getTotalCount() {
                return totalCount;
            }
        };
    }

    private com.sallejoven.backend.repository.projection.WarningTotalsProjection warningTotals(Long yellow, Long red) {
        return new com.sallejoven.backend.repository.projection.WarningTotalsProjection() {
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
}
