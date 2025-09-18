// UserDto.java
package com.sallejoven.backend.model.dto;

import com.sallejoven.backend.model.dto.abstractDto.BaseUserDto;
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
public class UserDto extends BaseUserDto {
    private int userType;
}
