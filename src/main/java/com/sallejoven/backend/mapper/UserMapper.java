package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.UserDto;
import com.sallejoven.backend.model.dto.UserPendingDto;
import com.sallejoven.backend.model.dto.UserResponse;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.entity.UserSalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(UserSalle user);

    @Mapping(target = "rol", ignore = true)
    UserSelfDto toUserSelfDto(UserSalle user);

    @Mapping(target = "userType", source = "userType")
    UserDto toUserDto(UserSalle user, int userType);

    @Mapping(target = "uuid", source = "user.uuid")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "dni", source = "user.dni")
    @Mapping(target = "phone", source = "user.phone")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "tshirtSize", source = "user.tshirtSize")
    @Mapping(target = "healthCardNumber", source = "user.healthCardNumber")
    @Mapping(target = "intolerances", source = "user.intolerances")
    @Mapping(target = "chronicDiseases", source = "user.chronicDiseases")
    @Mapping(target = "imageAuthorization", source = "user.imageAuthorization")
    @Mapping(target = "birthDate", source = "user.birthDate")
    @Mapping(target = "gender", source = "user.gender")
    @Mapping(target = "address", source = "user.address")
    @Mapping(target = "city", source = "user.city")
    @Mapping(target = "motherFullName", source = "user.motherFullName")
    @Mapping(target = "fatherFullName", source = "user.fatherFullName")
    @Mapping(target = "motherEmail", source = "user.motherEmail")
    @Mapping(target = "fatherEmail", source = "user.fatherEmail")
    @Mapping(target = "fatherPhone", source = "user.fatherPhone")
    @Mapping(target = "motherPhone", source = "user.motherPhone")
    @Mapping(target = "userType", source = "userType")
    UserDto toUserDtoFromUserGroup(UserGroup userGroup);

    @Mapping(target = "rol", ignore = true)
    @Mapping(target = "center", ignore = true)
    @Mapping(target = "stage", ignore = true)
    UserPendingDto toUserPendingDto(UserPending pending);
}
