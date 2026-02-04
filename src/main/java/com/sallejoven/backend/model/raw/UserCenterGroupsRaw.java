package com.sallejoven.backend.model.raw;

import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.Center;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserCenterGroupsRaw {
    private final Center center;
    private final List<UserGroupDto> groups;
}