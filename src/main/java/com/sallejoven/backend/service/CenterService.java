package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.enums.UserType;
import com.sallejoven.backend.model.raw.UserCenterGroupsRaw;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.service.AcademicStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;
    private final GroupService groupService;
    private final UserCenterService userCenterService;
    private final AcademicStateService academicStateService;
    private final UserGroupRepository userGroupRepository;

    public Center findById(Long id) throws SalleException {
        return centerRepository.findById(id)
                .orElseThrow(() -> new SalleException(ErrorCodes.CENTER_NOT_FOUND));
    }

    public List<Center> getAllCentersWithGroups() {
        return centerRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<GroupSalle> getGroupsForCenter(Center center) {
        return groupService.findByCenter(center);
    }

    public List<UserCenterGroupsRaw> getCentersForUserRaw(UserSalle me, List<Role> roles) throws SalleException {
        Role mainRole = (roles == null || roles.isEmpty()) ? Role.PARTICIPANT : Collections.min(roles);

        switch (mainRole) {
            case ADMIN:
                return rawForAdmin();

            case GROUP_LEADER:
            case PASTORAL_DELEGATE:
                return rawForLeaderOrDelegate(me);

            default:
                return rawForDefaultUser(me);
        }
    }

    private List<UserCenterGroupsRaw> rawForAdmin() throws SalleException {
        int year = academicStateService.getVisibleYear();
        List<UserCenterGroupsRaw> raws = new ArrayList<>();
        for (Center c : getAllCentersWithGroups()) {
            List<GroupSalle> groups = groupService.findGroupsByCenterIdForYear(c.getId(), year);
            List<UserGroupDto> dtos = groups.stream()
                    .map(g -> UserGroupDto.builder()
                            .id(g.getId() != null ? g.getId().intValue() : null)
                            .groupId(g.getId() != null ? g.getId().intValue() : null)
                            .stage(g.getStage())
                            .user_type(UserType.ADMIN.toInt())
                            .build())
                    .toList();
            raws.add(new UserCenterGroupsRaw(c, dtos));
        }
        return raws;
    }

    private List<UserCenterGroupsRaw> rawForLeaderOrDelegate(UserSalle me) throws SalleException {
        List<UserCenterGroupsRaw> result = new ArrayList<>();
        List<UserCenter> myCenters = userCenterService.findByUserForCurrentYear(me.getId());

        Map<Long, UserCenter> chosenByCenter = new LinkedHashMap<>();
        for (UserCenter uc : myCenters) {
            Integer t = uc.getUserType();
            if (t != null && (t == 2 || t == 3)) {
                Long centerId = uc.getCenter().getId();
                UserCenter prev = chosenByCenter.get(centerId);
                if (prev == null || prev.getUserType() != 3) {
                    chosenByCenter.put(centerId, uc);
                }
            }
        }

        int year = academicStateService.getVisibleYear();
        Set<Long> mainCenterIds = new HashSet<>(chosenByCenter.keySet());
        for (UserCenter uc : chosenByCenter.values()) {
            Center c = uc.getCenter();
            Long centerId = c.getId();
            List<GroupSalle> groups = groupService.findGroupsByCenterIdForYear(centerId, year);
            int roleCode = uc.getUserType() != null ? uc.getUserType() : UserType.PARTICIPANT.toInt();
            List<UserGroupDto> dtos = groups.stream()
                    .map(g -> UserGroupDto.builder()
                            .id(g.getId() != null ? g.getId().intValue() : null)
                            .groupId(g.getId() != null ? g.getId().intValue() : null)
                            .stage(g.getStage())
                            .user_type(roleCode)
                            .build())
                    .toList();
            result.add(new UserCenterGroupsRaw(c, dtos));
        }

        List<UserCenterGroupsRaw> others = rawForDefaultUser(me);
        if (!mainCenterIds.isEmpty()) {
            others.removeIf(raw -> mainCenterIds.contains(raw.getCenter().getId()));
        }

        result.addAll(others);
        return result;
    }

    private List<UserCenterGroupsRaw> rawForDefaultUser(UserSalle me) throws SalleException {
        int year = academicStateService.getVisibleYear();
        List<UserGroup> active = userGroupRepository.findByUser_IdAndYearAndDeletedAtIsNull(me.getId(), year).stream()
                .filter(ug -> ug.getGroup() != null && ug.getGroup().getCenter() != null)
                .toList();

        // agrupamos por centro y devolvemos un raw por centro
        Map<Center, List<UserGroup>> byCenter = active.stream()
                .collect(Collectors.groupingBy(ug -> ug.getGroup().getCenter()));

        List<UserCenterGroupsRaw> raws = new ArrayList<>();
        for (Map.Entry<Center, List<UserGroup>> e : byCenter.entrySet()) {
            // Para usuario “normal” ponemos el tipo PARTICIPANT (ajusta si tienes otra regla)
            List<UserGroupDto> dtos = e.getValue().stream()
                    .map(ug -> {
                        GroupSalle g = ug.getGroup();
                        Integer ut = ug.getUserType();
                        return UserGroupDto.builder()
                                .id(g.getId() != null ? g.getId().intValue() : null)
                                .groupId(g.getId() != null ? g.getId().intValue() : null)
                                .stage(g.getStage())
                                .user_type(ut != null ? ut : UserType.PARTICIPANT.toInt())
                                .build();
                    })
                    .toList();
            raws.add(new UserCenterGroupsRaw(e.getKey(), dtos));
        }
        return raws;
    }

}