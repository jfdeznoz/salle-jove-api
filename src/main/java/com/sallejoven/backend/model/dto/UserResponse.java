package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sallejoven.backend.model.entity.UserSalle;

import java.time.LocalDateTime;
import java.util.Date;

public record UserResponse(
        java.util.UUID uuid,
        String name,
        String lastName,
        String email,
        String phone,
        String dni,
        Integer tshirtSize,
        String healthCardNumber,
        String intolerances,
        String chronicDiseases,
        Boolean imageAuthorization,
        @JsonFormat(pattern = "yyyy-MM-dd") Date birthDate,
        Integer gender,
        String address,
        String city,
        String motherFullName,
        String fatherFullName,
        String motherEmail,
        String fatherEmail,
        String motherPhone,
        String fatherPhone,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime deletedAt
) {
    public static UserResponse from(UserSalle user) {
        return new UserResponse(
                user.getUuid(),
                user.getName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getDni(),
                user.getTshirtSize(),
                user.getHealthCardNumber(),
                user.getIntolerances(),
                user.getChronicDiseases(),
                user.getImageAuthorization(),
                user.getBirthDate(),
                user.getGender(),
                user.getAddress(),
                user.getCity(),
                user.getMotherFullName(),
                user.getFatherFullName(),
                user.getMotherEmail(),
                user.getFatherEmail(),
                user.getMotherPhone(),
                user.getFatherPhone(),
                user.getDeletedAt()
        );
    }
}
