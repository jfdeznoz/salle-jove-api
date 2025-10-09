package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.UserCenterDto;
import com.sallejoven.backend.model.dto.UserCenterGroupsDto;
import com.sallejoven.backend.model.dto.UserGroupDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserCenter;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.enums.UserType;
import com.sallejoven.backend.model.raw.UserCenterGroupsRaw;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.CenterRepository;
import com.sallejoven.backend.utils.SalleConverters;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;
    private final GroupService groupService;
    private final UserCenterService userCenterService;

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

    private List<UserCenterGroupsRaw> rawForAdmin() {
        List<UserCenterGroupsRaw> raws = new ArrayList<>();
        for (Center c : getAllCentersWithGroups()) {
            List<GroupSalle> groups = groupService.findGroupsByCenterId(c.getId());
            raws.add(new UserCenterGroupsRaw(c, groups, UserType.ADMIN.toInt())); // 4
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

        Set<Long> mainCenterIds = new HashSet<>(chosenByCenter.keySet());
        for (UserCenter uc : chosenByCenter.values()) {
            Center c = uc.getCenter();
            Long centerId = c.getId();
            List<GroupSalle> groups = groupService.findGroupsByCenterId(centerId);
            result.add(new UserCenterGroupsRaw(c, groups, uc.getUserType())); // 2 ó 3
        }

        List<UserCenterGroupsRaw> others = rawForDefaultUser(me);
        if (!mainCenterIds.isEmpty()) {
            others.removeIf(raw -> mainCenterIds.contains(raw.getCenter().getId()));
        }

        result.addAll(others);
        return result;
    }

    private List<UserCenterGroupsRaw> rawForDefaultUser(UserSalle me) {
        List<UserGroup> active = me.getGroups().stream()
                .filter(this::isActive)
                .toList();

        // agrupamos por centro y devolvemos un raw por centro
        Map<Center, List<GroupSalle>> byCenter = active.stream()
                .filter(ug -> ug.getGroup() != null && ug.getGroup().getCenter() != null)
                .collect(Collectors.groupingBy(
                        ug -> ug.getGroup().getCenter(),
                        Collectors.mapping(UserGroup::getGroup, Collectors.toList())
                ));

        List<UserCenterGroupsRaw> raws = new ArrayList<>();
        for (Map.Entry<Center, List<GroupSalle>> e : byCenter.entrySet()) {
            // Para usuario “normal” ponemos el tipo PARTICIPANT (ajusta si tienes otra regla)
            raws.add(new UserCenterGroupsRaw(e.getKey(), e.getValue(), UserType.PARTICIPANT.toInt()));
        }
        return raws;
    }

    private boolean isActive(UserGroup ug) {
        return ug != null && ug.getDeletedAt() == null && ug.getGroup() != null;
    }

}