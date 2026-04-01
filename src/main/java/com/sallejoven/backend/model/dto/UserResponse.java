package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sallejoven.backend.model.entity.UserSalle;

import java.util.Date;

public record UserResponse(
        Long id,
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
        String fatherPhone
) {
    public static UserResponse from(UserSalle user) {
        return new UserResponse(
                user.getId(),
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
                user.getFatherPhone()
        );
    }
}
