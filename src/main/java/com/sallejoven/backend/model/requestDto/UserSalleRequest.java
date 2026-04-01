package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    private String dni;

    @Size(max = 20)
    private String phone;

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    private Integer tshirtSize;

    @Size(max = 50)
    private String healthCardNumber;

    @Size(max = 500)
    private String intolerances;

    @Size(max = 200)
    private String address;

    @Size(max = 500)
    private String chronicDiseases;

    @Size(max = 100)
    private String city;

    private Boolean imageAuthorization;

    @NotNull
    private Date birthDate;

    private Integer gender;

    @Size(max = 200)
    private String motherFullName;

    @Size(max = 200)
    private String fatherFullName;

    @Email @Size(max = 150)
    private String motherEmail;

    @Email @Size(max = 150)
    private String fatherEmail;

    @Size(max = 20)
    private String fatherPhone;

    @Size(max = 20)
    private String motherPhone;

    private List<Long> groups;

    private String rol;

    private Integer eventId;

    private Integer groupId;

    private Integer centerId;

    @Size(min = 8, max = 200)
    private String password;
}
