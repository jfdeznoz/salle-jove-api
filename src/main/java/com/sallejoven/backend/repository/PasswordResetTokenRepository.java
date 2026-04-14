package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);

    void deleteAllByExpiresAtBefore(Instant cutoff);
}
