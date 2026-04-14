package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.VitalSituationDto;
import com.sallejoven.backend.model.dto.VitalSituationSessionDto;
import com.sallejoven.backend.model.entity.VitalSituation;
import com.sallejoven.backend.model.entity.VitalSituationSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VitalSituationMapper {

    VitalSituationDto toDto(VitalSituation vitalSituation);

    @Mapping(target = "vitalSituationUuid", source = "vitalSituation.uuid")
    VitalSituationSessionDto toSessionDto(VitalSituationSession session);
}
