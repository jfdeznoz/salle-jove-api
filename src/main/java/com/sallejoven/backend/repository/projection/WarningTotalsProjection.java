package com.sallejoven.backend.repository.projection;

import java.util.UUID;

public interface WarningTotalsProjection {
    UUID getReferenceUuid();
    Long getYellowCount();
    Long getRedCount();
}
