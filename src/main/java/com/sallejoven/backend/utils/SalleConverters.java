package com.sallejoven.backend.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sallejoven.backend.model.dto.UserCenterDto;
import com.sallejoven.backend.model.dto.UserCenterGroupsDto;
import com.sallejoven.backend.model.dto.UserDto;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.dto.UserPendingDto;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.enums.UserType;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.EventDto;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.dto.ParticipantDto;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventUser;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.repository.GroupRepository;
import com.sallejoven.backend.service.AuthService;
import com.sallejoven.backend.service.UserService;


@Service
@RequiredArgsConstructor
public class SalleConverters {

    private final UserService userService;
    private final AuthService authService;
    private final CenterService centerService;
    private final GroupService groupService;

    public UserSelfDto buildSelfUserInfo(String userEmail) throws SalleException {
        UserSalle userTango = userService.findByEmail(userEmail);

        List<Role> roles = authService.getCurrentUserRoles();
        Role mainRole = roles.isEmpty() ? Role.PARTICIPANT : Collections.min(roles);

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
            .rol(mainRole)
            .gender(userTango.getGender())
            .address(userTango.getAddress())
            .city(userTango.getCity())
            .motherFullName(userTango.getMotherFullName())
            .fatherFullName(userTango.getFatherFullName())
            .motherEmail(userTango.getMotherEmail())
            .fatherEmail(userTango.getFatherEmail())
            .fatherPhone(userTango.getFatherPhone())
            .motherPhone(userTango.getMotherPhone())
            .build();
    }

    public UserSelfDto buildSelfUserInfo(UserSalle userTango) throws SalleException {
        List<Role> roles = userService.getUserRoles(userTango);
        Role mainRole = roles.isEmpty() ? Role.PARTICIPANT : roles.get(0);

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
            .rol(mainRole)
            .gender(userTango.getGender())
            .address(userTango.getAddress())
            .city(userTango.getCity())
            .motherFullName(userTango.getMotherFullName())
            .fatherFullName(userTango.getFatherFullName())
            .motherEmail(userTango.getMotherEmail())
            .fatherEmail(userTango.getFatherEmail())
            .fatherPhone(userTango.getFatherPhone())
            .motherPhone(userTango.getMotherPhone())
            .build();
    }

    public UserPendingDto userPendingToDto(UserPending userTango) throws SalleException {
        List<Role> roles = userService.getUserRoles(userTango);
        Role mainRole = roles.isEmpty() ? Role.PARTICIPANT : roles.get(0);

        Center center = null;
        GroupSalle group = null;

        if (userTango.getCenterId() != null) {
            center = centerService.findById(userTango.getCenterId());
        }

        if (userTango.getGroupId() != null) {
            group = groupService.findById(userTango.getGroupId());
            center = group.getCenter();
        }

        return UserPendingDto.builder()
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
                .rol(mainRole)
                .gender(userTango.getGender())
                .address(userTango.getAddress())
                .city(userTango.getCity())
                .motherFullName(userTango.getMotherFullName())
                .fatherFullName(userTango.getFatherFullName())
                .motherEmail(userTango.getMotherEmail())
                .fatherEmail(userTango.getFatherEmail())
                .fatherPhone(userTango.getFatherPhone())
                .motherPhone(userTango.getMotherPhone())
                .center(center != null ? center.getName() + " - " + center.getCity() : null)
                .stage(group != null ? group.getStage() : null)
                .build();
    }


    public UserDto buildSelfUserInfo(UserGroup userGroup) throws SalleException {
        UserSalle userTango = userGroup.getUser();

        return UserDto.builder()
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
                .gender(userTango.getGender())
                .address(userTango.getAddress())
                .city(userTango.getCity())
                .motherFullName(userTango.getMotherFullName())
                .fatherFullName(userTango.getFatherFullName())
                .motherEmail(userTango.getMotherEmail())
                .fatherEmail(userTango.getFatherEmail())
                .fatherPhone(userTango.getFatherPhone())
                .motherPhone(userTango.getMotherPhone())
                .userType(userGroup.getUserType())
                .build();
    }

    public UserGroupDto userGroupToDto(UserGroup userGroup){
        GroupSalle group = userGroup.getGroup();

        return UserGroupDto.builder()
                .groupId(group.getId().intValue())
                .stage(group.getStage())
                .user_type(userGroup.getUserType())
                .build();
    }

    public GroupDto groupToDto(GroupSalle group){
        Center center = group.getCenter();
        
        return GroupDto.builder()
            .groupId(group.getId().intValue())
            .stage(group.getStage())
            .centerName(center.getName() + " (" + center.getCity() + ")")
            .cityName(center.getCity())
            .centerId(center.getId().intValue())
            .build();
    }

    public UserGroupDto groupToUserGroupDto(GroupSalle group){
        return UserGroupDto.builder()
                .groupId(group.getId().intValue())
                .stage(group.getStage())
                .user_type(UserType.ADMIN.toInt())
                .build();
    }

    public EventDto eventToDto(Event event){

        return EventDto.builder()
                .eventId(event.getId().intValue())
                .name(event.getName())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .endDate(event.getEndDate())
                .fileName(event.getFileName())
                .stages(event.getStages())
                .place(event.getPlace())
                .isGeneral(event.getIsGeneral())
                .isBlocked(event.getIsBlocked())
                .build();
    }

    public ParticipantDto participantDto(EventUser eventUser) throws SalleException {
        UserGroup userGroup = eventUser.getUserGroup();
        UserSalle userSalle = userGroup.getUser();
        Integer userType = userGroup.getUserType();

        return ParticipantDto.builder()
                .userId(userSalle.getId())
                .name(userSalle.getName())
                .lastName(userSalle.getLastName())

                .dni(userSalle.getDni())
                .phone(userSalle.getPhone())
                .email(userSalle.getEmail())
                .tshirtSize(userSalle.getTshirtSize())
                .healthCardNumber(userSalle.getHealthCardNumber())
                .intolerances(userSalle.getIntolerances())
                .chronicDiseases(userSalle.getChronicDiseases())
                .city(userSalle.getCity())
                .address(userSalle.getAddress())
                .motherFullName(userSalle.getMotherFullName())
                .fatherFullName(userSalle.getFatherFullName())
                .motherEmail(userSalle.getMotherEmail())
                .fatherEmail(userSalle.getFatherEmail())
                .fatherPhone(userSalle.getFatherPhone())
                .motherPhone(userSalle.getMotherPhone())
                .birthDate(userSalle.getBirthDate())
                .gender(userSalle.getGender())
                .imageAuthorization(userSalle.getImageAuthorization())
                .attends(eventUser.getStatus())
                .userType(userType)
                .build();
    }

    public List<UserCenterGroupsDto> userGroupsToUserCenters(List<UserGroup> userGroups) {
        Map<Center, List<UserGroup>> byCenter = userGroups.stream()
                .collect(Collectors.groupingBy(ug -> ug.getGroup().getCenter()));

        return byCenter.entrySet().stream()
                .map(entry -> {
                    Center c = entry.getKey();
                    List<UserGroupDto> groupDtos = entry.getValue().stream()
                            .map(this::userGroupToDto)
                            .sorted(Comparator.comparing(UserGroupDto::getStage)) // opcional
                            .collect(Collectors.toList());

                    return UserCenterGroupsDto.builder()
                            .centerId(c.getId().intValue())
                            .centerName(c.getName())
                            .cityName(c.getCity())
                            .groups(groupDtos)
                            .build();
                })
                .sorted(Comparator.comparing(UserCenterGroupsDto::getCenterName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public UserCenterGroupsDto centerToUserCenterNoGroups(Center c) {
        return UserCenterGroupsDto.builder()
                .centerId(c.getId().intValue())
                .centerName(c.getName())
                .cityName(c.getCity() != null ? c.getCity() : null)
                .build();
    }

    public UserCenterGroupsDto toUserCenterGroupsDto(Center center, List<GroupSalle> groups, int roleCode) {
        UserCenterGroupsDto dto = centerToUserCenterNoGroups(center);

        List<UserGroupDto> groupDtos = new ArrayList<>(groups.size());
        for (GroupSalle g : groups) {
            groupDtos.add(UserGroupDto.builder()
                    .groupId(g.getId().intValue())
                    .stage(g.getStage())
                    .user_type(roleCode) // p.ej. ADMIN=4, DELEGATE=3, LEADER=2
                    .build());
        }

        groupDtos.sort((a, b) -> {
            int s = Comparator.nullsLast(Integer::compareTo).compare(a.getStage(), b.getStage());
            return (s != 0) ? s : Comparator.nullsLast(Integer::compareTo).compare(a.getGroupId(), b.getGroupId());
        });

        dto.setGroups(groupDtos);
        return dto;
    }

    public UserCenterDto userCenterToDto(UserCenter uc) {
        var center = uc.getCenter();
        return UserCenterDto.builder()
                .id(uc.getId())
                .centerId(center.getId() != null ? center.getId().intValue() : null)
                .centerName(center.getName())
                .cityName(center.getCity() != null ? center.getCity() : null)
                .userType(uc.getUserType()) // int (0..4)
                .build();
    }

    public UserDto userSalleToUserDto(UserSalle user, int userType) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .lastName(user.getLastName())
                .dni(user.getDni())
                .phone(user.getPhone())
                .email(user.getEmail())
                .tshirtSize(user.getTshirtSize())
                .healthCardNumber(user.getHealthCardNumber())
                .intolerances(user.getIntolerances())
                .chronicDiseases(user.getChronicDiseases())
                .imageAuthorization(user.getImageAuthorization())
                .birthDate(user.getBirthDate())

                .gender(user.getGender())
                .address(user.getAddress())
                .city(user.getCity())

                .motherFullName(user.getMotherFullName())
                .fatherFullName(user.getFatherFullName())
                .motherEmail(user.getMotherEmail())
                .fatherEmail(user.getFatherEmail())
                .fatherPhone(user.getFatherPhone())
                .motherPhone(user.getMotherPhone())

                .userType(userType) // 👈 añadimos el tipo de rol en el centro
                .build();
    }
}
