package com.sallejoven.backend.model.dto;

import java.util.Date;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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

    @Temporal(TemporalType.DATE)
    private Date eventDate;

    private String fileName;

    private String place;

    private Integer[] stages;

    private Boolean isGeneral;

    private Boolean isBlocked;
}
