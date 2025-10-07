package com.sallejoven.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCenterGroupsDto {
    private Integer centerId;
    private String centerName;
    private String cityName;
    private List<UserGroupDto> groups;
}