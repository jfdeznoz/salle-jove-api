package com.sallejoven.backend.utils;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.EventDto;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.dto.ParticipantDto;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.UserService;


@Service
public class SalleConverters {

    private final UserService userService;
    private final AuthService authService;

    @Autowired
    public SalleConverters(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    public UserSelfDto buildSelfUserInfo(String userEmail) throws SalleException {
        UserSalle userTango = userService.findByEmail(userEmail);

        List<GroupDto> groupDtos = userTango.getGroups().stream()
            .map(this::groupToDto)
            .toList();
        
            List<Role> roles = authService.getCurrentUserRoles();

        return UserSelfDto.builder()
            .id(userTango.getId())
            .name(userTango.getName())
            .lastName(userTango.getLastName())
            .dni(userTango.getDni())
            .phone(userTango.getPhone())
            .email(userTango.getEmail())
            .tshirtSize(userTango.getTshirtSize())
            .healthCardNumber(userTango.getHealthCardNumber())
            .intolerances(userTango.getIntolerances())
            .chronicDiseases(userTango.getChronicDiseases())
            .imageAuthorization(userTango.getImageAuthorization())
            .birthDate(userTango.getBirthDate())
            .groups(groupDtos)
            .rol(roles.getFirst())
            .build();
    }

    public UserSelfDto userToDto(UserSalle userSalle) {

        return UserSelfDto.builder()
            .id(userSalle.getId())
            .name(userSalle.getName())
            .lastName(userSalle.getLastName())
            .dni(userSalle.getDni())
            .phone(userSalle.getPhone())
            .email(userSalle.getEmail())
            .tshirtSize(userSalle.getTshirtSize())
            .healthCardNumber(userSalle.getHealthCardNumber())
            .intolerances(userSalle.getIntolerances())
            .chronicDiseases(userSalle.getChronicDiseases())
            .imageAuthorization(userSalle.getImageAuthorization())
            .birthDate(userSalle.getBirthDate())
            .build();
    }
    
    public GroupDto groupToDto(GroupSalle group){

        return GroupDto.builder()
        .groupId(group.getId().intValue())
                .stage(group.getStage())
                .centerName(group.getCenter().getName())
                .cityName(group.getCenter().getCity())
                .build();
    }

    public EventDto eventToDto(Event event){

        return EventDto.builder()
                .eventId(event.getId().intValue())
                .name(event.getName())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .fileName(event.getFileName())
                .stages(event.getStages())
                .place(event.getPlace())
                .build();
    }

    public ParticipantDto participantDto(EventUser eventUser){
        UserSalle userSalle = eventUser.getUser();
        return ParticipantDto.builder()
                .userId(userSalle.getId())
                .name(userSalle.getName())
                .lastName(userSalle.getLastName())
                .attends(eventUser.getStatus())
                .build();
    }
}
