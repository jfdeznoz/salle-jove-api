package com.sallejoven.backend.model.requestDto;

import java.util.Date;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSalleRequestOptional {
    private Optional<String> name = Optional.empty();
    private Optional<String> lastName = Optional.empty();
    private Optional<String> dni = Optional.empty();
    private Optional<String> phone = Optional.empty();
    private Optional<String> email = Optional.empty();
    private Optional<Integer> tshirtSize = Optional.empty();
    private Optional<String> healthCardNumber = Optional.empty();
    private Optional<String> intolerances = Optional.empty();
    private Optional<String> chronicDiseases = Optional.empty();
    private Optional<String> address = Optional.empty();
    private Optional<String> city = Optional.empty();
    private Optional<Boolean> imageAuthorization = Optional.empty();
    private Optional<Date> birthDate = Optional.empty();
    private Optional<Integer> gender = Optional.empty();
    private Optional<String> motherFullName = Optional.empty();
    private Optional<String> fatherFullName = Optional.empty();
    private Optional<String> motherEmail = Optional.empty();
    private Optional<String> fatherEmail = Optional.empty();
    private Optional<String> motherPhone = Optional.empty();
    private Optional<String> fatherPhone = Optional.empty();
}
