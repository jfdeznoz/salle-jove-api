package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sallejoven.backend.model.enums.WeeklySessionWarningType;

import java.util.Date;

public record ParticipantDto(
    java.util.UUID uuid,
    String name,
    String lastName,
    String dni,
    String phone,
    String email,
    Integer tshirtSize,
    String healthCardNumber,
    String intolerances,
    String chronicDiseases,
    String city,
    String address,
    String motherFullName,
    String fatherFullName,
    String motherEmail,
    String fatherEmail,
    String fatherPhone,
    String motherPhone,
    @JsonFormat(pattern = "yyyy-MM-dd") Date birthDate,
    Integer gender,
    Boolean imageAuthorization,
    Integer attends,
    Boolean justified,
    String justificationReason,
    WeeklySessionWarningType warningType,
    String warningComment,
    String warningCreatedByName,
    Integer userType
) {}
