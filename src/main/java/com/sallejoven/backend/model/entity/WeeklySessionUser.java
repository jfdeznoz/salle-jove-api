package com.sallejoven.backend.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "weekly_session_user",
        uniqueConstraints = @UniqueConstraint(name = "uq_weekly_session_user", columnNames = {"weekly_session_uuid", "user_uuid"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySessionUser {

    @Id
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "weekly_session_uuid", nullable = false, referencedColumnName = "uuid")
    private WeeklySession weeklySession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", nullable = false, referencedColumnName = "uuid")
    private UserSalle user;

    /** null=PENDING, 0=NO_ATTENDS, 1=ATTENDS */
    @Column(nullable = true)
    private Integer status;

    @Column(nullable = false)
    @Builder.Default
    private Boolean justified = false;

    @Column(name = "justification_reason", length = 300)
    private String justificationReason;

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
