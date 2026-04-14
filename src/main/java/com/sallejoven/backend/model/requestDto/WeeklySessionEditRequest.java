package com.sallejoven.backend.model.requestDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySessionEditRequest {

    private String uuid;

    private String vitalSituationSessionUuid;

    @Size(max = 150)
    private String title;

    private String groupUuid;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime sessionDateTime;

    @Size(max = 2000)
    private String observations;

    @Size(max = 2000)
    private String content;
}
