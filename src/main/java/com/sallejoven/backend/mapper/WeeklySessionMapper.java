package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.WeeklySessionDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.WeeklySession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeeklySessionMapper {

    @Mapping(target = "vitalSituationSessionId", source = "vitalSituationSession.id")
    @Mapping(target = "vitalSituationTitle", source = "vitalSituationSession.vitalSituation.title")
    @Mapping(target = "vitalSituationSessionTitle", source = "vitalSituationSession.title")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", expression = "java(formatGroupName(session))")
    @Mapping(target = "stage", source = "group.stage")
    @Mapping(target = "centerId", source = "group.center.id")
    @Mapping(target = "centerName", source = "group.center.name")
    WeeklySessionDto toDto(WeeklySession session);

    default String formatGroupName(WeeklySession session) {
        Center center = session.getGroup().getCenter();
        return center.getName() + " - " + center.getCity();
    }
}
