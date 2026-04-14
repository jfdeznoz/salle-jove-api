package com.sallejoven.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = "uuid")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vital_situation_session")
public class VitalSituationSession {

    @Id
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vital_situation_uuid", nullable = false, referencedColumnName = "uuid")
    private VitalSituation vitalSituation;

    @Column(nullable = false)
    private String title;

    @Column(name = "pdf")
    private String pdf;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
