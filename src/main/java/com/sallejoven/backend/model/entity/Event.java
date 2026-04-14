package com.sallejoven.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = "uuid")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event")
public class Event {

    @Id
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid;

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

    @jakarta.persistence.PrePersist
    private void ensureUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return uuid;
    }

    public void setId(UUID id) {
        this.uuid = id;
    }
}
