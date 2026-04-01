package com.sallejoven.backend.model.requestDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    private Long id;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 300)
    private String description;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate eventDate;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate endDate;

    @NotEmpty
    private List<Integer> stages;

    @Size(max = 150)
    private String place;

    private Boolean isGeneral;

    @Positive
    private Long centerId;

    @Size(max = 500)
    private String imageUpload;

    private Boolean wantPdfUpload;
}
