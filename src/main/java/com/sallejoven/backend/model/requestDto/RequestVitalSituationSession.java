package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestVitalSituationSession {

    private Long id;

    @NotNull
    private Long vitalSituationId;

    @NotNull
    private String title;

    private String pdfUpload;

    private Boolean wantPdfUpload;

    private Boolean isDefault;
}

