package com.sallejoven.backend.mapper;

import com.sallejoven.backend.model.dto.ParticipantDto;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParticipantMapper {

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
    @Mapping(target = "city", source = "user.city")
    @Mapping(target = "address", source = "user.address")
    @Mapping(target = "motherFullName", source = "user.motherFullName")
    @Mapping(target = "fatherFullName", source = "user.fatherFullName")
    @Mapping(target = "motherEmail", source = "user.motherEmail")
    @Mapping(target = "fatherEmail", source = "user.fatherEmail")
    @Mapping(target = "fatherPhone", source = "user.fatherPhone")
    @Mapping(target = "motherPhone", source = "user.motherPhone")
    @Mapping(target = "birthDate", source = "user.birthDate")
    @Mapping(target = "gender", source = "user.gender")
    @Mapping(target = "imageAuthorization", source = "user.imageAuthorization")
    @Mapping(target = "attends", source = "status")
    @Mapping(target = "justified", expression = "java(null)")
    @Mapping(target = "justificationReason", expression = "java(null)")
    @Mapping(target = "warningType", expression = "java(null)")
    @Mapping(target = "warningComment", expression = "java(null)")
    @Mapping(target = "userType", ignore = true)
    ParticipantDto toEventParticipant(EventUser eventUser);

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
    @Mapping(target = "city", source = "user.city")
    @Mapping(target = "address", source = "user.address")
    @Mapping(target = "motherFullName", source = "user.motherFullName")
    @Mapping(target = "fatherFullName", source = "user.fatherFullName")
    @Mapping(target = "motherEmail", source = "user.motherEmail")
    @Mapping(target = "fatherEmail", source = "user.fatherEmail")
    @Mapping(target = "fatherPhone", source = "user.fatherPhone")
    @Mapping(target = "motherPhone", source = "user.motherPhone")
    @Mapping(target = "birthDate", source = "user.birthDate")
    @Mapping(target = "gender", source = "user.gender")
    @Mapping(target = "imageAuthorization", source = "user.imageAuthorization")
    @Mapping(target = "attends", source = "status")
    @Mapping(target = "justified", source = "justified")
    @Mapping(target = "justificationReason", source = "justificationReason")
    @Mapping(target = "warningType", source = "behaviorWarning.warningType")
    @Mapping(target = "warningComment", source = "behaviorWarning.comment")
    @Mapping(target = "userType", ignore = true)
    ParticipantDto toWeeklySessionParticipant(WeeklySessionUser weeklySessionUser);

    default ParticipantDto fromEventUser(EventUser eventUser, Integer userType) {
        return withUserType(toEventParticipant(eventUser), userType);
    }

    default ParticipantDto fromWeeklySessionUser(WeeklySessionUser weeklySessionUser, Integer userType) {
        return withUserType(toWeeklySessionParticipant(weeklySessionUser), userType);
    }

    private ParticipantDto withUserType(ParticipantDto dto, Integer userType) {
        return new ParticipantDto(
                dto.uuid(),
                dto.name(),
                dto.lastName(),
                dto.dni(),
                dto.phone(),
                dto.email(),
                dto.tshirtSize(),
                dto.healthCardNumber(),
                dto.intolerances(),
                dto.chronicDiseases(),
                dto.city(),
                dto.address(),
                dto.motherFullName(),
                dto.fatherFullName(),
                dto.motherEmail(),
                dto.fatherEmail(),
                dto.fatherPhone(),
                dto.motherPhone(),
                dto.birthDate(),
                dto.gender(),
                dto.imageAuthorization(),
                dto.attends(),
                dto.justified(),
                dto.justificationReason(),
                dto.warningType(),
                dto.warningComment(),
                userType
        );
    }
}
