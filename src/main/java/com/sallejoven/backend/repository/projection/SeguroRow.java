package com.sallejoven.backend.repository.projection;

import java.time.LocalDate;

public interface SeguroRow {
    Long getUserId();
    String getName();
    String getLastName();
    LocalDate getBirthDate();
    String getDni();
    String getCentersGroups();
}