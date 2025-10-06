package com.sallejoven.backend.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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

@Entity
@Table(name = "user_center")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_salle", nullable = false)
    private UserSalle user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center", nullable = false)
    private Center center;

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
        if (userType == null || (userType != 2 && userType != 3)) {
            throw new IllegalArgumentException("User_type must be 2 (GROUP_LEADER) or 3 (PASTORAL_DELEGATE)");
        }
    }
}
