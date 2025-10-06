package com.sallejoven.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserCenterDto {
    private Long id;
    private Integer centerId;
    private String  centerName;
    private String  cityName;
    private Integer userType;
}
