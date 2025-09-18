// src/main/java/com/sallejoven/backend/model/dto/UserCenterDto.java
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
public class UserCenterDto {
    private Integer centerId;
    private String centerName;
    private String cityName;
    // Reutilizamos UserGroupDto para que incluya user_type, etc.
    private List<UserGroupDto> groups;
}