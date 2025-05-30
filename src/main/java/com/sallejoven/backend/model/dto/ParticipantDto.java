package com.sallejoven.backend.model.dto;

import com.sallejoven.backend.model.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantDto {

    private Long userId;

    private String name;

    private String lastName;

    private Integer attends;

    private Role rol;
}