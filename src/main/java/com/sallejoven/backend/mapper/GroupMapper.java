package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.dto.GroupResponse;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    @Mapping(target = "centerUuid", source = "center.uuid")
    @Mapping(target = "centerName", expression = "java(formatCenterName(group.getCenter()))")
    @Mapping(target = "cityName", source = "center.city")
    GroupDto toGroupDto(GroupSalle group);

    @Mapping(target = "centerUuid", source = "center.uuid")
    @Mapping(target = "centerName", source = "center.name")
    @Mapping(target = "cityName", source = "center.city")
    GroupResponse toGroupResponse(GroupSalle group);

    @Mapping(target = "userType", source = "userType")
    @Mapping(target = "groupUuid", source = "group.uuid")
    @Mapping(target = "uuid", source = "uuid")
    @Mapping(target = "stage", source = "group.stage")
    UserGroupDto toUserGroupDto(UserGroup userGroup);

    default String formatCenterName(Center center) {
        if (center == null) return null;
        String name = center.getName() != null ? center.getName() : "";
        String city = center.getCity();
        return (city != null && !city.isBlank()) ? name + " (" + city + ")" : name;
    }
}
