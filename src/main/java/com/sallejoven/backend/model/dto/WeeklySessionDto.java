package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeeklySessionDto {
    private Long id;
    private Long vitalSituationSessionId;
    private String vitalSituationTitle;
    private String vitalSituationSessionTitle;
    private String title;
    private Long groupId;
    private String groupName;
    private Integer stage;
    private Long centerId;
    private String centerName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime sessionDateTime;

    private Integer status; // 0=DRAFT, 1=PUBLISHED, 2=ARCHIVED
}

