package com.sallejoven.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSelfDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String lastName;

    private String dni;

    private String phone;

    private String email;

    private String tshirtSize;

    private String healthCardNumber;

    private String intolerances;

    private String chronicDiseases;

    private Boolean imageAuthorization;

    private Date birthDate;

}