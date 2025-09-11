package com.sallejoven.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserGroupDto {
    private Integer groupId;

    private Integer centerId;

    private Integer stage;

    private Integer user_type;

    private String centerName;

    private String cityName;
}