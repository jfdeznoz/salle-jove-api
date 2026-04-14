package com.sallejoven.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_group")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserGroup {

    @Id
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", nullable = false, referencedColumnName = "uuid")
    private UserSalle user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_uuid", nullable = false, referencedColumnName = "uuid")
    private GroupSalle group;

    /** 0=PARTICIPANT, 1=ANIMATOR */
    @Column(name = "user_type", nullable = false)
    private Integer userType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    @PreUpdate
    private void validateRole() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (userType == null || (userType != 0 && userType != 1)) {
            throw new IllegalArgumentException("User_type must be 0 (PARTICIPANT) or 1 (ANIMATOR)");
        }
    }

    public UUID getId() {
        return uuid;
    }

    public void setId(UUID id) {
        this.uuid = id;
    }
}
