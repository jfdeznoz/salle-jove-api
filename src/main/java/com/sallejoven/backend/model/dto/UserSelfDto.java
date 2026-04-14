package com.sallejoven.backend.model.dto;

import com.sallejoven.backend.model.dto.abstractDto.BaseUserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.sallejoven.backend.model.enums.Role;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserSelfDto extends BaseUserDto {
    private Role rol;
    // Enriched fields — only populated in search results
    private String centerName;
    private java.util.List<String> groupNames;
}