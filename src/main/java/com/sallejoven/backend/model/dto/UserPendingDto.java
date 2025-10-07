package com.sallejoven.backend.model.dto;

import com.sallejoven.backend.model.dto.abstractDto.BaseUserDto;
import com.sallejoven.backend.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserPendingDto extends BaseUserDto {
    private Role rol;
    private String center;
    private Integer stage;
}