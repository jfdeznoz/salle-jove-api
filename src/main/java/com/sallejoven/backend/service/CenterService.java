package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.mapper.CenterMapper;
import com.sallejoven.backend.model.dto.CenterDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(readOnly = true)
public class CenterService {

    private final CenterRepository centerRepository;
    private final GroupService groupService;
    private final UserCenterService userCenterService;
    private final AcademicStateService academicStateService;
    private final UserGroupRepository userGroupRepository;
    private final CenterMapper centerMapper;

    public Center findById(Long id) {
        return centerRepository.findById(id)
                .orElseThrow(() -> new SalleException(ErrorCodes.CENTER_NOT_FOUND));
    }

    public List<Center> getAllCentersWithGroups() {
        return centerRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<CenterDto> listMyCenters(UserSalle me, List<Role> roles) {
        if (roles.contains(Role.ADMIN)) {
            return getAllCentersWithGroups().stream()
                    .map(this::toCenterDto)
                    .toList();
        }
        return userCenterService.findByUserForCurrentYear(me.getId()).stream()
                .map(this::userCenterToCenterDto)
                .toList();
    }

    private CenterDto toCenterDto(Center center) {
        return centerMapper.toCenterDtoWithoutGroups(center);
    }

    private CenterDto userCenterToCenterDto(UserCenter uc) {
        return centerMapper.fromUserCenter(uc);
    }

    public List<UserGroup> getActiveUserGroupsForYear(UserSalle user) {
        int visibleYear = academicStateService.getVisibleYear();
        return user.getGroups().stream()
                .filter(ug -> ug.getDeletedAt() == null
                        && ug.getGroup() != null
                        && ug.getYear() == visibleYear)
                .toList();
    }

    public List<GroupSalle> getGroupsForCenter(Center center) {
        return groupService.findByCenter(center);
    }

    public List<UserCenterGroupsRaw> getCentersForUserRaw(UserSalle me, List<Role> roles) {
        Role mainRole = (roles == null || roles.isEmpty()) ? Role.PARTICIPANT : Collections.min(roles);

        return switch (mainRole) {
            case ADMIN -> rawForAdmin();
            case GROUP_LEADER, PASTORAL_DELEGATE -> rawForLeaderOrDelegate(me);
            default -> rawForDefaultUser(me);
        };
    }

    private List<UserCenterGroupsRaw> rawForAdmin() {
        int year = academicStateService.getVisibleYear();
        List<UserCenterGroupsRaw> raws = new ArrayList<>();
        for (Center c : getAllCentersWithGroups()) {
            List<GroupSalle> groups = groupService.findGroupsByCenterIdForYear(c.getId(), year);
            List<UserGroupDto> dtos = groups.stream()
                    .map(g -> new UserGroupDto(
                            UserType.ADMIN.toInt(),
                            g.getId(),
                            g.getId() != null ? g.getId().intValue() : null,
                            g.getStage()))
                    .toList();
            raws.add(new UserCenterGroupsRaw(c, dtos));
        }
        return raws;
    }

    private List<UserCenterGroupsRaw> rawForLeaderOrDelegate(UserSalle me) {
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
                    .map(g -> new UserGroupDto(
                            roleCode,
                            g.getId(),
                            g.getId() != null ? g.getId().intValue() : null,
                            g.getStage()))
                    .toList();
            result.add(new UserCenterGroupsRaw(c, dtos));
        }

        List<UserCenterGroupsRaw> others = rawForDefaultUser(me);
        if (!mainCenterIds.isEmpty()) {
            others.removeIf(raw -> mainCenterIds.contains(raw.center().getId()));
        }

        result.addAll(others);
        return result;
    }

    private List<UserCenterGroupsRaw> rawForDefaultUser(UserSalle me) {
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
                        return new UserGroupDto(
                                ut != null ? ut : UserType.PARTICIPANT.toInt(),
                                g.getId(),
                                g.getId() != null ? g.getId().intValue() : null,
                                g.getStage());
                    })
                    .toList();
            raws.add(new UserCenterGroupsRaw(e.getKey(), dtos));
        }
        return raws;
    }

}