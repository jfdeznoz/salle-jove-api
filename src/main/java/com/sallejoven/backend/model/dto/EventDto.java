package com.sallejoven.backend.model.dto;

import java.time.LocalDate;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventDto {
    private Integer eventId;
    
    private String name;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate eventDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String fileName;

    private String place;

    private Integer[] stages;

    private Boolean isGeneral;

    private Boolean isBlocked;

    private Integer centerId;

    private String centerName;

    private String pdf;
}
