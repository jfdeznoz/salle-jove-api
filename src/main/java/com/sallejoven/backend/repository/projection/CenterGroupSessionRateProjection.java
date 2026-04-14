package com.sallejoven.backend.repository.projection;

import java.util.UUID;

public interface CenterGroupSessionRateProjection {
    UUID getGroupUuid();

    Integer getStage();

    UUID getSessionUuid();

    Long getAttendedCount();

    Long getTotalCount();
}
