package com.sallejoven.backend.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface GroupSessionSummaryProjection {
    UUID getUuid();

    LocalDateTime getDate();

    String getTitle();

    String getVitalSituationTitle();

    String getContent();

    Long getAttendanceCount();

    Long getTotalCount();
}
