package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSituationSessionRequest {

    private Long id;

    @NotNull
    @Positive
    private Long vitalSituationId;

    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 500)
    private String pdfUpload;

    private Boolean wantPdfUpload;
}
