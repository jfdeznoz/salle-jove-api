package com.sallejoven.backend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record UserRegistrationDto (
    @NotEmpty(message = "User Name must not be empty")
    String userName,
    String userMobileNo,
    @NotEmpty(message = "User email must not be empty")
    @Email(message = "Invalid email format")
    String userEmail,

    @NotEmpty(message = "User password must not be empty")
    String userPassword
){ }