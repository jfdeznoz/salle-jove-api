package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.WeeklySessionDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.service.AcademicStateService;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.repository.WeeklySessionBehaviorWarningRepository;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class WeeklySessionMapper {

    @Autowired
    protected WeeklySessionUserRepository weeklySessionUserRepository;

    @Autowired
    protected AcademicStateService academicStateService;

    @Autowired
    protected WeeklySessionBehaviorWarningRepository weeklySessionBehaviorWarningRepository;

    @Autowired
    protected AuthService authService;

    @Mapping(target = "vitalSituationSessionUuid", source = "session.vitalSituationSession.uuid")
    @Mapping(target = "vitalSituationTitle", source = "session.vitalSituationSession.vitalSituation.title")
    @Mapping(target = "vitalSituationSessionTitle", source = "session.vitalSituationSession.title")
    @Mapping(target = "groupUuid", source = "session.group.uuid")
    @Mapping(target = "groupName", expression = "java(formatGroupName(session))")
    @Mapping(target = "stage", source = "session.group.stage")
    @Mapping(target = "centerUuid", source = "session.group.center.uuid")
    @Mapping(target = "centerName", source = "session.group.center.name")
    @Mapping(target = "observations", source = "session.observations")
    @Mapping(target = "content", source = "session.content")
    @Mapping(target = "yellowWarningCount", ignore = true)
    @Mapping(target = "redWarningCount", ignore = true)
    @Mapping(target = "currentUserAttendanceStatus", ignore = true)
    @Mapping(target = "currentUserJustified", ignore = true)
    @Mapping(target = "currentUserWarningType", ignore = true)
    protected abstract WeeklySessionDto toDto(WeeklySession session, Integer attendanceCount, Integer totalCount);

    public WeeklySessionDto toDto(WeeklySession session) {
        int visibleYear = academicStateService.getVisibleYear();
        WeeklySessionUserRepository.AttendanceCountProjection counts =
                weeklySessionUserRepository.countAttendanceBySessionUuid(session.getUuid(), visibleYear);
        var warningTotals = weeklySessionBehaviorWarningRepository.findSessionWarningTotals(session.getUuid());
        WeeklySessionUser currentUserSession = weeklySessionUserRepository
                .findBySessionUserAndGroup(
                        session.getUuid(),
                        authService.getCurrentUser().getUuid(),
                        session.getGroup().getUuid(),
                        visibleYear)
                .orElse(null);
        int attendanceCount = toInt(counts == null ? null : counts.getAttendanceCount());
        int totalCount = toInt(counts == null ? null : counts.getTotalCount());
        WeeklySessionDto dto = toDto(session, attendanceCount, totalCount);
        return new WeeklySessionDto(
                dto.uuid(),
                dto.vitalSituationSessionUuid(),
                dto.vitalSituationTitle(),
                dto.vitalSituationSessionTitle(),
                dto.title(),
                dto.groupUuid(),
                dto.groupName(),
                dto.stage(),
                dto.centerUuid(),
                dto.centerName(),
                dto.sessionDateTime(),
                dto.observations(),
                dto.content(),
                toInt(warningTotals == null ? null : warningTotals.getYellowCount()),
                toInt(warningTotals == null ? null : warningTotals.getRedCount()),
                dto.attendanceCount(),
                dto.totalCount(),
                dto.status(),
                currentUserSession == null ? null : currentUserSession.getStatus(),
                currentUserSession != null && Boolean.TRUE.equals(currentUserSession.getJustified()),
                currentUserSession != null
                        && currentUserSession.getBehaviorWarning() != null
                        && currentUserSession.getBehaviorWarning().getDeletedAt() == null
                        ? currentUserSession.getBehaviorWarning().getWarningType()
                        : null);
    }

    protected String formatGroupName(WeeklySession session) {
        Center center = session.getGroup().getCenter();
        return center.getName() + " - " + center.getCity();
    }

    private int toInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }
}
