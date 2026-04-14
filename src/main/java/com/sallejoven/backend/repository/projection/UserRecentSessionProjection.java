package com.sallejoven.backend.repository.projection;

import java.time.LocalDateTime;

public interface UserRecentSessionProjection {
    LocalDateTime getDate();
    String getTitle();
    String getVitalSituationTitle();
    String getVitalSituationSessionTitle();
    Boolean getAttended();
    Boolean getJustified();
}
