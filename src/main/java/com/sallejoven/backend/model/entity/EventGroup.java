package com.sallejoven.backend.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = "uuid")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "event_group",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_uuid", "group_uuid"})
)
public class EventGroup {

    @Id
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "event_uuid", nullable = false, referencedColumnName = "uuid")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "group_uuid", nullable = false, referencedColumnName = "uuid")
    private GroupSalle groupSalle;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
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
