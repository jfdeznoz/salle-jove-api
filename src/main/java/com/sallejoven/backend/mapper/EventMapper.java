package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.EventDto;
import com.sallejoven.backend.model.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "centerUuid", source = "centerUuid")
    @Mapping(target = "centerName", source = "centerName")
    EventDto toEventDto(Event event, java.util.UUID centerUuid, String centerName);
}
