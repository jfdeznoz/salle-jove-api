// BaseUserDto.java
package com.sallejoven.backend.model.dto.abstractDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseUserDto {
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
    private Boolean imageAuthorization;
    private Date birthDate;

    private Integer gender;
    private String address;
    private String city;

    private String motherFullName;
    private String fatherFullName;
    private String motherEmail;
    private String fatherEmail;
    private String fatherPhone;
    private String motherPhone;
}
