package com.sallejoven.backend.service;

import com.sallejoven.backend.model.dto.AdminOverviewDto;
import com.sallejoven.backend.model.dto.CenterAttendanceStatsDto;
import com.sallejoven.backend.model.dto.GroupAttendanceStatsDto;
import com.sallejoven.backend.model.dto.UserAttendanceStatsDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.repository.EventUserRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import com.sallejoven.backend.repository.projection.AttendanceTotalsProjection;
import com.sallejoven.backend.repository.projection.CenterGroupSessionRateProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final AcademicStateService academicStateService;
    private final UserService userService;
    private final GroupService groupService;
    private final CenterService centerService;
    private final WeeklySessionUserRepository weeklySessionUserRepository;
    private final EventUserRepository eventUserRepository;
    private final UserGroupRepository userGroupRepository;

    public UserAttendanceStatsDto getUserAttendanceStats(UUID userUuid, Integer year) {
        userService.findByUserId(userUuid);
        int resolvedYear = resolveYear(year);

        var sessionStats = weeklySessionUserRepository.findUserAttendanceStats(userUuid, resolvedYear);
        var eventStats = eventUserRepository.findUserAttendanceStats(userUuid, resolvedYear);

        List<UserAttendanceStatsDto.RecentSessionDto> recentSessions = weeklySessionUserRepository
                .findRecentSessionsByUser(userUuid, resolvedYear, PageRequest.of(0, 50))
                .stream()
                .map(row -> new UserAttendanceStatsDto.RecentSessionDto(
                        row.getDate(),
                        row.getTitle(),
                        row.getVitalSituationTitle(),
                        row.getVitalSituationSessionTitle(),
                        Boolean.TRUE.equals(row.getAttended()),
                        Boolean.TRUE.equals(row.getJustified())))
                .toList();

        return new UserAttendanceStatsDto(
                new UserAttendanceStatsDto.SessionAttendanceDto(
                        toInt(sessionStats == null ? null : sessionStats.getTotal()),
                        toInt(sessionStats == null ? null : sessionStats.getAttended()),
                        toInt(sessionStats == null ? null : sessionStats.getJustified()),
                        rate(
                                sessionStats == null ? null : sessionStats.getAttended(),
                                sessionStats == null ? null : sessionStats.getTotal())),
                new UserAttendanceStatsDto.EventAttendanceDto(
                        toInt(eventStats == null ? null : eventStats.getTotal()),
                        toInt(eventStats == null ? null : eventStats.getAttended()),
                        rate(
                                eventStats == null ? null : eventStats.getAttended(),
                                eventStats == null ? null : eventStats.getTotal())),
                recentSessions
        );
    }

    public List<Integer> getAvailableYears(UUID userUuid) {
        return userGroupRepository.findDistinctYearsByUserUuid(userUuid);
    }

    public GroupAttendanceStatsDto getGroupAttendanceStats(UUID groupUuid, Integer year) {
        groupService.findById(groupUuid);
        int resolvedYear = resolveYear(year);

        List<GroupAttendanceStatsDto.SessionSummaryDto> sessions = weeklySessionUserRepository
                .findGroupSessionSummaries(groupUuid, resolvedYear)
                .stream()
                .map(row -> new GroupAttendanceStatsDto.SessionSummaryDto(
                        row.getUuid(),
                        row.getDate(),
                        row.getTitle(),
                        row.getVitalSituationTitle(),
                        row.getContent(),
                        toInt(row.getAttendanceCount()),
                        toInt(row.getTotalCount())))
                .toList();

        List<GroupAttendanceStatsDto.MemberAttendanceDto> members = weeklySessionUserRepository
                .findGroupMemberAttendance(groupUuid, resolvedYear)
                .stream()
                .map(row -> new GroupAttendanceStatsDto.MemberAttendanceDto(
                        row.getUserUuid(),
                        row.getName(),
                        row.getLastName(),
                        toInt(row.getSessionsAttended()),
                        toInt(row.getSessionsTotal()),
                        rate(row.getSessionsAttended(), row.getSessionsTotal())))
                .toList();

        return new GroupAttendanceStatsDto(sessions, members);
    }

    public CenterAttendanceStatsDto getCenterAttendanceStats(UUID centerUuid, Integer year) {
        centerService.findById(centerUuid);
        int resolvedYear = resolveYear(year);

        Map<UUID, GroupRateAccumulator> grouped = new LinkedHashMap<>();
        for (CenterGroupSessionRateProjection row : weeklySessionUserRepository.findCenterGroupSessionRates(centerUuid, resolvedYear)) {
            GroupRateAccumulator accumulator = grouped.computeIfAbsent(
                    row.getGroupUuid(),
                    ignored -> new GroupRateAccumulator(row.getStage()));
            accumulator.add(row.getSessionUuid(), rate(row.getAttendedCount(), row.getTotalCount()));
        }

        List<CenterAttendanceStatsDto.GroupStatsDto> groups = grouped.entrySet().stream()
                .map(entry -> new CenterAttendanceStatsDto.GroupStatsDto(
                        entry.getKey(),
                        entry.getValue().stage(),
                        entry.getValue().sessionsCount(),
                        entry.getValue().averageRate()))
                .sorted(Comparator.comparing(
                        CenterAttendanceStatsDto.GroupStatsDto::stage,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();

        AttendanceTotalsProjection overallTotals = weeklySessionUserRepository.findCenterAttendanceTotals(centerUuid, resolvedYear);
        return new CenterAttendanceStatsDto(groups, rate(overallTotals == null ? null : overallTotals.getAttended(),
                overallTotals == null ? null : overallTotals.getTotal()));
    }

    public AdminOverviewDto getAdminOverview(Integer year) {
        int resolvedYear = resolveYear(year);

        List<AdminOverviewDto.CenterOverviewDto> centers = centerService.getAllCentersWithGroups().stream()
                .map(center -> buildCenterOverview(center, resolvedYear))
                .toList();

        List<AdminOverviewDto.CenterOverviewDto> topCenters = centers.stream()
                .sorted(Comparator.comparing(AdminOverviewDto.CenterOverviewDto::sessionRate,
                        Comparator.nullsLast(Double::compareTo)).reversed()
                        .thenComparing(AdminOverviewDto.CenterOverviewDto::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(5)
                .toList();

        List<AdminOverviewDto.CenterOverviewDto> bottomCenters = centers.stream()
                .sorted(Comparator.comparing(AdminOverviewDto.CenterOverviewDto::sessionRate,
                        Comparator.nullsLast(Double::compareTo))
                        .thenComparing(AdminOverviewDto.CenterOverviewDto::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(5)
                .toList();

        AttendanceTotalsProjection globalSessionTotals = weeklySessionUserRepository.findGlobalAttendanceTotals(resolvedYear);
        AttendanceTotalsProjection globalEventTotals = eventUserRepository.findGlobalAttendanceTotals(resolvedYear);

        return new AdminOverviewDto(
                centers,
                topCenters,
                bottomCenters,
                rate(globalSessionTotals == null ? null : globalSessionTotals.getAttended(),
                        globalSessionTotals == null ? null : globalSessionTotals.getTotal()),
                rate(globalEventTotals == null ? null : globalEventTotals.getAttended(),
                        globalEventTotals == null ? null : globalEventTotals.getTotal()),
                toInt(userGroupRepository.countDistinctUsersByYear(resolvedYear)),
                toInt(weeklySessionUserRepository.countDistinctSessionsByYear(resolvedYear)),
                toInt(eventUserRepository.countDistinctEventsByYear(resolvedYear))
        );
    }

    private AdminOverviewDto.CenterOverviewDto buildCenterOverview(Center center, int year) {
        int groupCount = userGroupRepository.findDistinctGroupUuidsByCenterUuidAndYear(center.getUuid(), year).size();
        int memberCount = toInt(userGroupRepository.countDistinctUsersByCenterUuidAndYear(center.getUuid(), year));

        AttendanceTotalsProjection sessionTotals = weeklySessionUserRepository.findCenterAttendanceTotals(center.getUuid(), year);
        AttendanceTotalsProjection eventTotals = eventUserRepository.findCenterAttendanceTotals(center.getUuid(), year);

        return new AdminOverviewDto.CenterOverviewDto(
                center.getUuid(),
                center.getName(),
                center.getCity(),
                groupCount,
                memberCount,
                rate(sessionTotals == null ? null : sessionTotals.getAttended(),
                        sessionTotals == null ? null : sessionTotals.getTotal()),
                rate(eventTotals == null ? null : eventTotals.getAttended(),
                        eventTotals == null ? null : eventTotals.getTotal())
        );
    }

    private int toInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }

    private Double rate(Long attended, Long total) {
        if (attended == null || total == null || total == 0L) {
            return 0.0;
        }
        return attended.doubleValue() / total.doubleValue();
    }

    private int resolveYear(Integer year) {
        return year == null ? academicStateService.getVisibleYear() : year;
    }

    private static final class GroupRateAccumulator {
        private final Integer stage;
        private final List<Double> sessionRates = new ArrayList<>();
        private final List<UUID> sessionUuids = new ArrayList<>();

        private GroupRateAccumulator(Integer stage) {
            this.stage = stage;
        }

        private void add(UUID sessionUuid, Double sessionRate) {
            if (!sessionUuids.contains(sessionUuid)) {
                sessionUuids.add(sessionUuid);
                sessionRates.add(sessionRate == null ? 0.0 : sessionRate);
            }
        }

        private Integer stage() {
            return stage;
        }

        private int sessionsCount() {
            return sessionUuids.size();
        }

        private double averageRate() {
            if (sessionRates.isEmpty()) {
                return 0.0;
            }
            return sessionRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
    }
}
