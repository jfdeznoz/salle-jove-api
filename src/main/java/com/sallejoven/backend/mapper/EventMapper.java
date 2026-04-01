package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.EventDto;
import com.sallejoven.backend.model.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "centerId", source = "centerId")
    @Mapping(target = "centerName", source = "centerName")
    EventDto toEventDto(Event event, Long centerId, String centerName);
}
