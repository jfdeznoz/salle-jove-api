package com.sallejoven.backend.repository.projection;

public interface UserSessionAttendanceStatsProjection {
    Long getTotal();
    Long getAttended();
    Long getJustified();
}
