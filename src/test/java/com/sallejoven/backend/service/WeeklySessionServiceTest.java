package com.sallejoven.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sallejoven.backend.mapper.WeeklySessionMapper;
import com.sallejoven.backend.model.dto.WeeklySessionSummaryDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.repository.VitalSituationSessionRepository;
import com.sallejoven.backend.repository.WeeklySessionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class WeeklySessionServiceTest {

    @Mock WeeklySessionRepository weeklySessionRepository;
    @Mock VitalSituationSessionRepository vitalSituationSessionRepository;
    @Mock GroupRepository groupRepository;
    @Mock UserGroupService userGroupService;
    @Mock WeeklySessionUserService weeklySessionUserService;
    @Mock AuthService authService;
    @Mock GroupService groupService;
    @Mock WeeklySessionMapper weeklySessionMapper;

    @InjectMocks WeeklySessionService weeklySessionService;

    @Test
    void findById_returnsDraftSession_forAnimator() {
        UUID sessionUuid = UUID.randomUUID();
        WeeklySession session = WeeklySession.builder()
                .uuid(sessionUuid)
                .status(0)
                .title("Sesion")
                .sessionDateTime(LocalDateTime.now())
                .build();

        when(weeklySessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));

        assertThat(weeklySessionService.findById(sessionUuid)).contains(session);
    }

    @Test
    void findAll_doesNotForcePublishedFilter_forAnimator() {
        UUID groupUuid = UUID.randomUUID();
        UserSalle user = new UserSalle();
        user.setUuid(UUID.randomUUID());
        user.setIsAdmin(false);

        GroupSalle group = GroupSalle.builder().uuid(groupUuid).build();
        Page<WeeklySession> expectedPage = new PageImpl<>(List.of());

        when(authService.getCurrentUser()).thenReturn(user);
        when(groupService.findEffectiveGroupsForUser(user)).thenReturn(List.of(group));
        when(weeklySessionRepository.findByGroupsAndPastStatus(
                List.of(group),
                false,
                LocalDate.now(java.time.ZoneId.of("Europe/Madrid")),
                false,
                PageRequest.of(0, 10)))
                .thenReturn(expectedPage);

        assertThat(weeklySessionService.findAll(0, 10, false, null, null)).isSameAs(expectedPage);
    }

    @Test
    void getWeeklySummaryByGroupIds_returnsAggregatedCountsAndZeroDefaults() {
        UUID firstGroupUuid = UUID.randomUUID();
        UUID secondGroupUuid = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Madrid"));
        LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate currentWeekEnd = currentWeekStart.plusDays(6);
        LocalDate previousWeekStart = currentWeekStart.minusWeeks(1);
        LocalDate previousWeekEnd = previousWeekStart.plusDays(6);

        WeeklySessionRepository.WeeklySessionSummaryProjection row = new WeeklySessionRepository.WeeklySessionSummaryProjection() {
            @Override
            public UUID getGroupUuid() {
                return firstGroupUuid;
            }

            @Override
            public Long getCurrentWeekCount() {
                return 3L;
            }

            @Override
            public Long getPreviousWeekCount() {
                return 1L;
            }
        };

        when(weeklySessionRepository.summarizeWeeklySessionsByGroup(
                List.of(firstGroupUuid, secondGroupUuid),
                currentWeekStart,
                currentWeekEnd,
                previousWeekStart,
                previousWeekEnd))
                .thenReturn(List.of(row));

        Map<UUID, WeeklySessionSummaryDto> summary =
                weeklySessionService.getWeeklySummaryByGroupIds(List.of(firstGroupUuid, secondGroupUuid));

        assertThat(summary).containsEntry(firstGroupUuid, new WeeklySessionSummaryDto(3, 1));
        assertThat(summary).containsEntry(secondGroupUuid, new WeeklySessionSummaryDto(0, 0));
    }
}
