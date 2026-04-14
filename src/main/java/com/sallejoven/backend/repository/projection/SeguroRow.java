package com.sallejoven.backend.repository.projection;

import java.time.LocalDate;
import java.util.UUID;

public interface SeguroRow {
    UUID getUserUuid();

    String getName();

    String getLastName();

    LocalDate getBirthDate();

    String getDni();

    String getCentersGroups();

    Integer getUserType();
}
