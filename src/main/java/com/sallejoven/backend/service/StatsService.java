package com.sallejoven.backend.service;

import com.sallejoven.backend.model.dto.AdminOverviewDto;
import com.sallejoven.backend.model.dto.CenterAttendanceStatsDto;
import com.sallejoven.backend.model.dto.GroupAttendanceStatsDto;
import com.sallejoven.backend.model.dto.UserAttendanceStatsDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.repository.WeeklySessionBehaviorWarningRepository;
import com.sallejoven.backend.repository.EventUserRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.WeeklySessionRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import com.sallejoven.backend.repository.projection.AttendanceTotalsProjection;
import com.sallejoven.backend.repository.projection.CenterGroupSessionRateProjection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final WeeklySessionRepository weeklySessionRepository;
    private final WeeklySessionBehaviorWarningRepository weeklySessionBehaviorWarningRepository;
    private final EventUserRepository eventUserRepository;
    private final UserGroupRepository userGroupRepository;

    public UserAttendanceStatsDto getUserAttendanceStats(UUID userUuid, Integer year) {
        userService.findByUserId(userUuid);
        int resolvedYear = resolveYear(year);
        LocalDateTime startOfToday = startOfTodayMadrid();
        LocalDateTime academicYearStart = startOfAcademicYear(resolvedYear);
        LocalDateTime nextAcademicYearStart = startOfAcademicYear(resolvedYear + 1);

        var sessionStats = weeklySessionUserRepository.findUserAttendanceStats(
                userUuid,
                academicYearStart,
                nextAcademicYearStart,
                startOfToday);
        var eventStats = eventUserRepository.findUserAttendanceStats(userUuid, resolvedYear);
        var warningStats = weeklySessionBehaviorWarningRepository.findUserWarningTotals(
                userUuid,
                academicYearStart,
                nextAcademicYearStart);
        List<UserAttendanceStatsDto.AcademicGroupDto> memberships = userGroupRepository
                .findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, resolvedYear)
                .stream()
                .map(ug -> new UserAttendanceStatsDto.AcademicGroupDto(
                        ug.getGroup().getCenter().getName(),
                        ug.getGroup().getStage(),
                        ug.getUserType()))
                .distinct()
                .sorted(Comparator.comparing(
                                UserAttendanceStatsDto.AcademicGroupDto::centerName,
                                Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(
                                UserAttendanceStatsDto.AcademicGroupDto::stage,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(
                                UserAttendanceStatsDto.AcademicGroupDto::userType,
                                Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<UserAttendanceStatsDto.RecentSessionDto> recentSessions = weeklySessionUserRepository
                .findRecentSessionsByUser(
                        userUuid,
                        academicYearStart,
                        nextAcademicYearStart,
                        startOfToday,
                        PageRequest.of(0, 50))
                .stream()
                .map(row -> new UserAttendanceStatsDto.RecentSessionDto(
                        row.getSessionUuid(),
                        row.getDate(),
                        row.getTitle(),
                        row.getVitalSituationTitle(),
                        row.getVitalSituationSessionTitle(),
                        row.getAttended(),
                        Boolean.TRUE.equals(row.getJustified()),
                        row.getWarningType()))
                .toList();

        List<UserAttendanceStatsDto.LedSessionDto> ledSessions = weeklySessionRepository
                .findLedSessionsByUser(
                        userUuid,
                        resolvedYear,
                        academicYearStart,
                        nextAcademicYearStart,
                        PageRequest.of(0, 100))
                .stream()
                .map(row -> new UserAttendanceStatsDto.LedSessionDto(
                        row.getSessionUuid(),
                        row.getDate(),
                        row.getTitle(),
                        row.getVitalSituationTitle(),
                        row.getVitalSituationSessionTitle(),
                        row.getContent(),
                        row.getObservations(),
                        row.getCenterName(),
                        row.getStage()))
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
                new UserAttendanceStatsDto.WarningStatsDto(
                        toInt(warningStats == null ? null : warningStats.getYellowCount()),
                        toInt(warningStats == null ? null : warningStats.getRedCount()),
                        toInt((warningStats == null ? null : warningStats.getYellowCount()))
                                + toInt((warningStats == null ? null : warningStats.getRedCount()))),
                memberships,
                recentSessions,
                ledSessions
        );
    }

    public List<Integer> getAvailableYears(UUID userUuid) {
        return userGroupRepository.findDistinctYearsByUserUuid(userUuid);
    }

    public GroupAttendanceStatsDto getGroupAttendanceStats(UUID groupUuid, Integer year) {
        groupService.findById(groupUuid);
        int resolvedYear = resolveYear(year);
        LocalDateTime startOfToday = startOfTodayMadrid();

        List<GroupAttendanceStatsDto.SessionSummaryDto> sessions = weeklySessionUserRepository
                .findGroupSessionSummaries(groupUuid, resolvedYear)
                .stream()
                .map(row -> new GroupAttendanceStatsDto.SessionSummaryDto(
                        row.getUuid(),
                        row.getDate(),
                        row.getTitle(),
                        row.getVitalSituationTitle(),
                        row.getContent(),
                        0,
                        0,
                        toInt(row.getAttendanceCount()),
                        toInt(row.getTotalCount())))
                .toList();

        Map<UUID, int[]> warningsBySession = weeklySessionBehaviorWarningRepository
                .findGroupSessionWarningTotals(groupUuid, resolvedYear)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.getReferenceUuid(),
                        row -> new int[] {toInt(row.getYellowCount()), toInt(row.getRedCount())}));

        sessions = sessions.stream()
                .map(session -> {
                    int[] warningCounts = warningsBySession.getOrDefault(session.uuid(), new int[] {0, 0});
                    return new GroupAttendanceStatsDto.SessionSummaryDto(
                            session.uuid(),
                            session.date(),
                            session.title(),
                            session.vitalSituationTitle(),
                            session.content(),
                            warningCounts[0],
                            warningCounts[1],
                            session.attendanceCount(),
                            session.totalCount());
                })
                .toList();

        Map<UUID, int[]> warningsByMember = weeklySessionBehaviorWarningRepository
                .findGroupMemberWarningTotals(groupUuid, resolvedYear)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.getReferenceUuid(),
                        row -> new int[] {toInt(row.getYellowCount()), toInt(row.getRedCount())}));

        List<GroupAttendanceStatsDto.MemberAttendanceDto> members = weeklySessionUserRepository
                .findGroupMemberAttendance(groupUuid, resolvedYear, startOfToday)
                .stream()
                .map(row -> new GroupAttendanceStatsDto.MemberAttendanceDto(
                        row.getUserUuid(),
                        row.getName(),
                        row.getLastName(),
                        warningsByMember.getOrDefault(row.getUserUuid(), new int[] {0, 0})[0],
                        warningsByMember.getOrDefault(row.getUserUuid(), new int[] {0, 0})[1],
                        toInt(row.getSessionsAttended()),
                        toInt(row.getSessionsTotal()),
                        rate(row.getSessionsAttended(), row.getSessionsTotal())))
                .toList();

        return new GroupAttendanceStatsDto(sessions, members);
    }

    public CenterAttendanceStatsDto getCenterAttendanceStats(UUID centerUuid, Integer year) {
        centerService.findById(centerUuid);
        int resolvedYear = resolveYear(year);
        LocalDateTime startOfToday = startOfTodayMadrid();

        Map<UUID, GroupRateAccumulator> grouped = new LinkedHashMap<>();
        for (CenterGroupSessionRateProjection row : weeklySessionUserRepository.findCenterGroupSessionRates(centerUuid, resolvedYear, startOfToday)) {
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
                        entry.getValue().averageRate(),
                        0,
                        0))
                .sorted(Comparator.comparing(
                        CenterAttendanceStatsDto.GroupStatsDto::stage,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();

        Map<UUID, int[]> warningsByGroup = weeklySessionBehaviorWarningRepository.findCenterGroupWarningTotals(centerUuid, resolvedYear)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.getGroupUuid(),
                        row -> new int[] {toInt(row.getYellowCount()), toInt(row.getRedCount())}));

        groups = groups.stream()
                .map(group -> {
                    int[] warningCounts = warningsByGroup.getOrDefault(group.groupUuid(), new int[] {0, 0});
                    return new CenterAttendanceStatsDto.GroupStatsDto(
                            group.groupUuid(),
                            group.stage(),
                            group.sessionsCount(),
                            group.avgAttendanceRate(),
                            warningCounts[0],
                            warningCounts[1]);
                })
                .toList();

        AttendanceTotalsProjection overallTotals = weeklySessionUserRepository.findCenterAttendanceTotals(centerUuid, resolvedYear, startOfToday);
        var overallWarnings = weeklySessionBehaviorWarningRepository.findCenterWarningTotals(centerUuid, resolvedYear);
        return new CenterAttendanceStatsDto(
                groups,
                rate(overallTotals == null ? null : overallTotals.getAttended(),
                        overallTotals == null ? null : overallTotals.getTotal()),
                toInt(overallWarnings == null ? null : overallWarnings.getYellowCount()),
                toInt(overallWarnings == null ? null : overallWarnings.getRedCount()));
    }

    public AdminOverviewDto getAdminOverview(Integer year) {
        int resolvedYear = resolveYear(year);
        LocalDateTime startOfToday = startOfTodayMadrid();

        List<AdminOverviewDto.CenterOverviewDto> centers = centerService.getAllCentersWithGroups().stream()
                .map(center -> buildCenterOverview(center, resolvedYear, startOfToday))
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

        AttendanceTotalsProjection globalSessionTotals = weeklySessionUserRepository.findGlobalAttendanceTotals(resolvedYear, startOfToday);
        AttendanceTotalsProjection globalEventTotals = eventUserRepository.findGlobalAttendanceTotals(resolvedYear);
        var globalWarnings = weeklySessionBehaviorWarningRepository.findGlobalWarningTotals(resolvedYear);

        return new AdminOverviewDto(
                centers,
                topCenters,
                bottomCenters,
                rate(globalSessionTotals == null ? null : globalSessionTotals.getAttended(),
                        globalSessionTotals == null ? null : globalSessionTotals.getTotal()),
                rate(globalEventTotals == null ? null : globalEventTotals.getAttended(),
                        globalEventTotals == null ? null : globalEventTotals.getTotal()),
                toInt(globalWarnings == null ? null : globalWarnings.getYellowCount()),
                toInt(globalWarnings == null ? null : globalWarnings.getRedCount()),
                toInt(userGroupRepository.countDistinctUsersByYear(resolvedYear)),
                toInt(weeklySessionUserRepository.countDistinctSessionsByYear(resolvedYear)),
                toInt(eventUserRepository.countDistinctEventsByYear(resolvedYear))
        );
    }

    private AdminOverviewDto.CenterOverviewDto buildCenterOverview(Center center, int year, LocalDateTime startOfToday) {
        int groupCount = userGroupRepository.findDistinctGroupUuidsByCenterUuidAndYear(center.getUuid(), year).size();
        int memberCount = toInt(userGroupRepository.countDistinctUsersByCenterUuidAndYear(center.getUuid(), year));

        AttendanceTotalsProjection sessionTotals = weeklySessionUserRepository.findCenterAttendanceTotals(center.getUuid(), year, startOfToday);
        AttendanceTotalsProjection eventTotals = eventUserRepository.findCenterAttendanceTotals(center.getUuid(), year);
        var warningTotals = weeklySessionBehaviorWarningRepository.findCenterWarningTotals(center.getUuid(), year);

        return new AdminOverviewDto.CenterOverviewDto(
                center.getUuid(),
                center.getName(),
                center.getCity(),
                groupCount,
                memberCount,
                rate(sessionTotals == null ? null : sessionTotals.getAttended(),
                        sessionTotals == null ? null : sessionTotals.getTotal()),
                rate(eventTotals == null ? null : eventTotals.getAttended(),
                        eventTotals == null ? null : eventTotals.getTotal()),
                toInt(warningTotals == null ? null : warningTotals.getYellowCount()),
                toInt(warningTotals == null ? null : warningTotals.getRedCount())
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

    private LocalDateTime startOfTodayMadrid() {
        return java.time.ZonedDateTime.now(ZoneId.of("Europe/Madrid"))
                .toLocalDate()
                .atStartOfDay();
    }

    private LocalDateTime startOfAcademicYear(int year) {
        return LocalDate.of(year, 9, 1).atStartOfDay();
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
