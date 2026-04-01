package com.sallejoven.backend.model.requestDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class WeeklySessionRequest {

    private Long id;

    @NotNull
    @Positive
    private Long vitalSituationSessionId;

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotNull
    @Positive
    private Long groupId;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime sessionDateTime;
}
