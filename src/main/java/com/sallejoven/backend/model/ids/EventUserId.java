package com.sallejoven.backend.model.ids;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class EventUserId implements Serializable {
    private Long event;
    private Long userSalle;
}
