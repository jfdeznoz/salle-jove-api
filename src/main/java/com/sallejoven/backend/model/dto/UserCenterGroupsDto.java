package com.sallejoven.backend.model.dto;

import java.util.List;

public record UserCenterGroupsDto(
    Long centerId,
    String centerName,
    String cityName,
    List<UserGroupDto> groups
) {}
