package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.CenterDto;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.dto.UserCenterDto;
import com.sallejoven.backend.model.dto.UserCenterGroupsDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.UserCenter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CenterMapper {

    @Mapping(target = "groups", ignore = true)
    CenterDto toCenterDtoWithoutGroups(Center center);

    @Mapping(target = "name", source = "center.name")
    @Mapping(target = "city", source = "center.city")
    @Mapping(target = "groups", ignore = true)
    CenterDto fromUserCenter(UserCenter userCenter);

    @Mapping(target = "centerUuid", source = "center.uuid")
    @Mapping(target = "centerName", source = "center.name")
    @Mapping(target = "cityName", source = "center.city")
    UserCenterDto toUserCenterDto(UserCenter userCenter);

    @Mapping(target = "centerUuid", source = "uuid")
    @Mapping(target = "centerName", source = "name")
    @Mapping(target = "cityName", source = "city")
    @Mapping(target = "groups", ignore = true)
    @Mapping(target = "weeklySessionSummary", ignore = true)
    UserCenterGroupsDto toUserCenterGroupsDtoNoGroups(Center center);

    default CenterDto toCenterDtoWithGroups(Center center, List<GroupDto> groups) {
        return new CenterDto(center.getUuid(), center.getName(), center.getCity(), groups);
    }
}
