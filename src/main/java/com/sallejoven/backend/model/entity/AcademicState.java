// src/main/java/.../model/entity/AcademicState.java
package com.sallejoven.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "academic_state")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AcademicState {

    @Id
    private Short id = 1;

    @Column(name = "visible_year", nullable = false)
    private Integer visibleYear;

    @Column(name = "promoted_at")
    private OffsetDateTime promotedAt;
}
