package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;

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
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC")
    private Date eventDate;

    private List<Integer> stages;

    private MultipartFile file;

    private String place;
}