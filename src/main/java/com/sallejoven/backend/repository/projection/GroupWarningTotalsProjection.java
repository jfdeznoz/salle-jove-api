package com.sallejoven.backend.repository.projection;

import java.util.UUID;

public interface GroupWarningTotalsProjection {
    UUID getGroupUuid();
    Integer getStage();
    Long getYellowCount();
    Long getRedCount();
}
