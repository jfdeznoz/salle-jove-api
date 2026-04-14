package com.sallejoven.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// PasswordResetToken.java
@Getter
@Setter
@EqualsAndHashCode(of = "uuid")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(columnList = "token", unique = true),
        @Index(columnList = "expiresAt"),
        @Index(columnList = "email")
})
public class PasswordResetToken {
    @Id
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid;

    @Column(nullable = false, unique = true, length = 200)
    private String token; // base64url

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @PrePersist
    void ensureUuid() {
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
