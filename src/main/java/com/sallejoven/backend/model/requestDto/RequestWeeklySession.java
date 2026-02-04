package com.sallejoven.backend.model.requestDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestWeeklySession {

    private Long id;

    @NotNull
    private Long vitalSituationSessionId;

    @NotNull
    private String title;

    @NotNull
    private Long groupId;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime sessionDateTime;

    private Integer status; // 0=DRAFT, 1=PUBLISHED, 2=ARCHIVED
}

