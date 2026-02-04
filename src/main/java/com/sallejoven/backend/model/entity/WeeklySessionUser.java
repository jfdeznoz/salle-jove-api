package com.sallejoven.backend.model.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "weekly_session_user",
        uniqueConstraints = @UniqueConstraint(name = "uq_weekly_session_user", columnNames = {"weekly_session_id", "user_group_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySessionUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "weekly_session_id", nullable = false)
    private WeeklySession weeklySession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_group_id", nullable = false)
    private UserGroup userGroup;

    /** 0=NO_ATTENDS, 1=ATTENDS */
    @Column(nullable = false)
    private int status;

    @Column(name = "deleted_at")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;
}

