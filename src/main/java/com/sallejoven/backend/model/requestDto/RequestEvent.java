package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestEvent {

    private Long id;

    @NotNull
    private String name;

    private String description;

    @NotNull
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date eventDate;

    @NotNull
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date endDate;

    private List<Integer> stages;

    private MultipartFile file;

    private String place;

    private Boolean isGeneral;
}