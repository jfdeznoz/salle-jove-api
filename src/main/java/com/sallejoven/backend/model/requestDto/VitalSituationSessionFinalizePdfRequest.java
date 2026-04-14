package com.sallejoven.backend.model.requestDto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

public record VitalSituationSessionFinalizePdfRequest(
        @JsonAlias("pdfUpload")
        @Size(max = 500) String pdfKey
) {}
