package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteAllByExpiresAtBefore(Instant cutoff); // útil para limpieza programada
}
