package com.sallejoven.backend.model.raw;

import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.Center;

import java.util.List;

public record UserCenterGroupsRaw(Center center, List<UserGroupDto> groups) {}
