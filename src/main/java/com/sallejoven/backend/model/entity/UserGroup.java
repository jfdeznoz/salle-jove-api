package com.sallejoven.backend.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_group")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_salle", nullable = false)
    private UserSalle user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_salle", nullable = false)
    private GroupSalle group;

    /** 0=PARTICIPANT, 1=ANIMATOR */
    @Column(name = "user_type", nullable = false)
    private Integer userType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "deleted_at")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;

    @PrePersist
    @PreUpdate
    private void validateRole() {
        if (userType == null || (userType != 0 && userType != 1)) {
            throw new IllegalArgumentException("User_type must be 0 (PARTICIPANT) or 1 (ANIMATOR)");
        }
    }
}