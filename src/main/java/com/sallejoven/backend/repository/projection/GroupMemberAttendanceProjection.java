package com.sallejoven.backend.repository.projection;

import java.util.UUID;

public interface GroupMemberAttendanceProjection {
    UUID getUserUuid();

    String getName();

    String getLastName();

    Long getSessionsAttended();

    Long getSessionsTotal();
}
