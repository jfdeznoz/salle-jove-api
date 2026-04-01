package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.ParticipantDto;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParticipantMapper {

    @Mapping(target = "id", source = "userGroup.user.id")
    @Mapping(target = "name", source = "userGroup.user.name")
    @Mapping(target = "lastName", source = "userGroup.user.lastName")
    @Mapping(target = "dni", source = "userGroup.user.dni")
    @Mapping(target = "phone", source = "userGroup.user.phone")
    @Mapping(target = "email", source = "userGroup.user.email")
    @Mapping(target = "tshirtSize", source = "userGroup.user.tshirtSize")
    @Mapping(target = "healthCardNumber", source = "userGroup.user.healthCardNumber")
    @Mapping(target = "intolerances", source = "userGroup.user.intolerances")
    @Mapping(target = "chronicDiseases", source = "userGroup.user.chronicDiseases")
    @Mapping(target = "city", source = "userGroup.user.city")
    @Mapping(target = "address", source = "userGroup.user.address")
    @Mapping(target = "motherFullName", source = "userGroup.user.motherFullName")
    @Mapping(target = "fatherFullName", source = "userGroup.user.fatherFullName")
    @Mapping(target = "motherEmail", source = "userGroup.user.motherEmail")
    @Mapping(target = "fatherEmail", source = "userGroup.user.fatherEmail")
    @Mapping(target = "fatherPhone", source = "userGroup.user.fatherPhone")
    @Mapping(target = "motherPhone", source = "userGroup.user.motherPhone")
    @Mapping(target = "birthDate", source = "userGroup.user.birthDate")
    @Mapping(target = "gender", source = "userGroup.user.gender")
    @Mapping(target = "imageAuthorization", source = "userGroup.user.imageAuthorization")
    @Mapping(target = "attends", source = "status")
    @Mapping(target = "userType", source = "userGroup.userType")
    ParticipantDto fromEventUser(EventUser eventUser);

    @Mapping(target = "id", source = "userGroup.user.id")
    @Mapping(target = "name", source = "userGroup.user.name")
    @Mapping(target = "lastName", source = "userGroup.user.lastName")
    @Mapping(target = "dni", source = "userGroup.user.dni")
    @Mapping(target = "phone", source = "userGroup.user.phone")
    @Mapping(target = "email", source = "userGroup.user.email")
    @Mapping(target = "tshirtSize", source = "userGroup.user.tshirtSize")
    @Mapping(target = "healthCardNumber", source = "userGroup.user.healthCardNumber")
    @Mapping(target = "intolerances", source = "userGroup.user.intolerances")
    @Mapping(target = "chronicDiseases", source = "userGroup.user.chronicDiseases")
    @Mapping(target = "city", source = "userGroup.user.city")
    @Mapping(target = "address", source = "userGroup.user.address")
    @Mapping(target = "motherFullName", source = "userGroup.user.motherFullName")
    @Mapping(target = "fatherFullName", source = "userGroup.user.fatherFullName")
    @Mapping(target = "motherEmail", source = "userGroup.user.motherEmail")
    @Mapping(target = "fatherEmail", source = "userGroup.user.fatherEmail")
    @Mapping(target = "fatherPhone", source = "userGroup.user.fatherPhone")
    @Mapping(target = "motherPhone", source = "userGroup.user.motherPhone")
    @Mapping(target = "birthDate", source = "userGroup.user.birthDate")
    @Mapping(target = "gender", source = "userGroup.user.gender")
    @Mapping(target = "imageAuthorization", source = "userGroup.user.imageAuthorization")
    @Mapping(target = "attends", source = "status")
    @Mapping(target = "userType", source = "userGroup.userType")
    ParticipantDto fromWeeklySessionUser(WeeklySessionUser weeklySessionUser);
}
