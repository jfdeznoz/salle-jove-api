package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.WeeklySessionDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.service.AcademicStateService;
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
    protected abstract WeeklySessionDto toDto(WeeklySession session, Integer attendanceCount, Integer totalCount);

    public WeeklySessionDto toDto(WeeklySession session) {
        WeeklySessionUserRepository.AttendanceCountProjection counts =
                weeklySessionUserRepository.countAttendanceBySessionUuid(session.getUuid(), academicStateService.getVisibleYear());
        int attendanceCount = toInt(counts == null ? null : counts.getAttendanceCount());
        int totalCount = toInt(counts == null ? null : counts.getTotalCount());
        return toDto(session, attendanceCount, totalCount);
    }

    protected String formatGroupName(WeeklySession session) {
        Center center = session.getGroup().getCenter();
        return center.getName() + " - " + center.getCity();
    }

    private int toInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }
}
