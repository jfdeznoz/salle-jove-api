package com.sallejoven.backend.service;

import com.sallejoven.backend.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordResetCleanup {
    private final PasswordResetTokenRepository repo;

    @Scheduled(cron = "0 0 3 * * MON", zone = "Europe/Madrid")
    public void cleanup() {
        repo.deleteAllByExpiresAtBefore(Instant.now().minus(Duration.ofDays(1)));
    }
}
