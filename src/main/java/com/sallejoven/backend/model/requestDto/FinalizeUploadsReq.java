package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.Size;

public record FinalizeUploadsReq(
        @Size(max = 500) String imageKey,
        @Size(max = 500) String pdfKey
) {}
