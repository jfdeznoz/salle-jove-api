package com.sallejoven.backend.mapper;

import org.springframework.stereotype.Component;

import com.sallejoven.backend.model.dto.UserRegistrationDto;
import com.sallejoven.backend.model.entity.UserSalle;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserInfoMapper {

    private final PasswordEncoder passwordEncoder;
    public UserSalle convertToEntity(UserRegistrationDto userRegistrationDto) {
        UserSalle userInfoEntity = new UserSalle();
        userInfoEntity.setEmail(userRegistrationDto.userEmail());
        userInfoEntity.setPhone(userRegistrationDto.userMobileNo());
        userInfoEntity.setPassword(passwordEncoder.encode(userRegistrationDto.userPassword()));
        return userInfoEntity;
    }
}
