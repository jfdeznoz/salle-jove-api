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

    /** 0=PARTICIPANT, 1=ANIMATOR, 2=GROUP_LEADER, 3=PASTORAL_DELEGATE */
    @Column(name = "user_type", nullable = false)
    private Integer userType;

    @Column(name = "deleted_at")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;
}