package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;

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

    private String fileName; 

    private String place;
}