package com.sallejoven.backend.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UserLedSessionProjection {
    UUID getSessionUuid();
    LocalDateTime getDate();
    String getTitle();
    String getVitalSituationTitle();
    String getVitalSituationSessionTitle();
    String getContent();
    String getObservations();
    String getCenterName();
    Integer getStage();
}
