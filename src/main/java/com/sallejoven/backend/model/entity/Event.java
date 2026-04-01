package com.sallejoven.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private Boolean divided;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "pdf")
    private String pdf;

    private String place;

    @Column(name = "stages", columnDefinition = "INT[]")
    private Integer[] stages;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_general")
    private Boolean isGeneral;

    @Column(name = "is_blocked")
    private Boolean isBlocked;
}