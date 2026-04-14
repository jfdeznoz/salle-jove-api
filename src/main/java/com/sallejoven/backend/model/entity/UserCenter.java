package com.sallejoven.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_center")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCenter {

    @Id
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", nullable = false, referencedColumnName = "uuid")
    private UserSalle user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_uuid", nullable = false, referencedColumnName = "uuid")
    private Center center;

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
        if (userType == null || (userType != 2 && userType != 3)) {
            throw new IllegalArgumentException("User_type must be 2 (GROUP_LEADER) or 3 (PASTORAL_DELEGATE)");
        }
    }

    public UUID getId() {
        return uuid;
    }

    public void setId(UUID id) {
        this.uuid = id;
    }
}
