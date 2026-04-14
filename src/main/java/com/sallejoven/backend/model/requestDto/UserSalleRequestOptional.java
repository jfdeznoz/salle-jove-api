package com.sallejoven.backend.model.requestDto;

import java.util.Date;
import java.util.Optional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSalleRequestOptional {
    private Optional<@Size(max = 100) String> name = Optional.empty();
    private Optional<@Size(max = 100) String> lastName = Optional.empty();
    private Optional<@Size(max = 20) String> dni = Optional.empty();
    private Optional<@Size(max = 20) String> phone = Optional.empty();
    private Optional<@Email @Size(max = 150) String> email = Optional.empty();
    private Optional<Integer> tshirtSize = Optional.empty();
    private Optional<@Size(max = 50) String> healthCardNumber = Optional.empty();
    private Optional<@Size(max = 500) String> intolerances = Optional.empty();
    private Optional<@Size(max = 500) String> chronicDiseases = Optional.empty();
    private Optional<@Size(max = 200) String> address = Optional.empty();
    private Optional<@Size(max = 100) String> city = Optional.empty();
    private Optional<Boolean> imageAuthorization = Optional.empty();
    private Optional<Date> birthDate = Optional.empty();
    private Optional<Integer> gender = Optional.empty();
    private Optional<@Size(max = 150) String> motherFullName = Optional.empty();
    private Optional<@Size(max = 150) String> fatherFullName = Optional.empty();
    private Optional<@Email @Size(max = 150) String> motherEmail = Optional.empty();
    private Optional<@Email @Size(max = 150) String> fatherEmail = Optional.empty();
    private Optional<@Size(max = 20) String> motherPhone = Optional.empty();
    private Optional<@Size(max = 20) String> fatherPhone = Optional.empty();
}
