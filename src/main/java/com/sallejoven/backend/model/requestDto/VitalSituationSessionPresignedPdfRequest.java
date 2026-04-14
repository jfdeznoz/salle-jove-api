package com.sallejoven.backend.model.requestDto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VitalSituationSessionPresignedPdfRequest(
        @JsonAlias("pdfUpload")
        @NotBlank
        @Size(max = 500) String pdfOriginalName
) {}
