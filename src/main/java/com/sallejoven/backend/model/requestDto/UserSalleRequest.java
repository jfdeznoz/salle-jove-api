package com.sallejoven.backend.model.requestDto;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSalleRequest {
    private String name;

    private String lastName;

    private String dni;

    private String phone;

    private String email;

    private Integer tshirtSize;

    private String healthCardNumber;

    private String intolerances;

    private String address;

    private String chronicDiseases;

    private String city;

    private Boolean imageAuthorization;

    private Date birthDate;

    private Integer gender;

    private String motherFullName;

    private String fatherFullName;

    private String motherEmail;

    private String fatherEmail;

    private String fatherPhone;

    private String motherPhone;

    private List<Long> groups;

    private String rol;

    private Integer eventId;

    private Integer groupId;

    private Integer centerId;
}
