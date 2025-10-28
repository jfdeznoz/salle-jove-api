package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sallejoven.backend.model.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantDto {

    private Long id;

    private String name;

    private String lastName;

    private String dni;
    private String phone;
    private String email;
    private Integer tshirtSize;
    private String healthCardNumber;
    private String intolerances;
    private String chronicDiseases;
    private String city;
    private String address;
    private String motherFullName;
    private String fatherFullName;
    private String motherEmail;
    private String fatherEmail;
    private String fatherPhone;
    private String motherPhone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthDate;
    private Integer gender;
    private Boolean imageAuthorization;

    private Integer attends;
    private Integer userType;
}