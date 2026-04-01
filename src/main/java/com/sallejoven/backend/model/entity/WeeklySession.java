package com.sallejoven.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "weekly_session")
public class WeeklySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vital_situation_session_id", nullable = false)
    private VitalSituationSession vitalSituationSession;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_salle_id", nullable = false)
    private GroupSalle group;

    @Column(name = "session_datetime", nullable = false)
    private LocalDateTime sessionDateTime;

    /** 0=DRAFT, 1=PUBLISHED, 2=ARCHIVED */
    @Column(nullable = false)
    private Integer status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

