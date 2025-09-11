package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.Event;
import com.sallejoven.backend.model.entity.EventGroup;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final EventGroupService eventGroupService;
    private final AuthService authService;

    @Autowired
    public GroupService(GroupRepository groupRepository, EventGroupService eventGroupService, AuthService authService) {
        this.groupRepository = groupRepository;
        this.eventGroupService = eventGroupService;
        this.authService = authService;
    }

    public Optional<GroupSalle> findById(Long id) {
        return groupRepository.findById(id);
    }

    public List<GroupSalle> findAll() {
        return groupRepository.findAll();
    }

    public Set<GroupSalle> findAllByIds(List<Long> ids) {
        return new HashSet<>(groupRepository.findAllById(ids));
    }

    public List<GroupSalle> findByCenter(Center center) {
        return groupRepository.findByCenter(center);
    }

    public List<GroupSalle> findGroupsByCenterId(Long centerId) {
        return groupRepository.findByCenterId(centerId);
    }

    public List<GroupSalle> findAllByEvent(Long eventId) throws SalleException {
        List<Role> roles = authService.getCurrentUserRoles();
        List<EventGroup> eventGroups = new ArrayList<>();

        if(roles.contains(Role.ADMIN)){
            eventGroups = eventGroupService.getEventGroupsByEventId(eventId);
        }else if(roles.contains(Role.PASTORAL_DELEGATE) || roles.contains(Role.GROUP_LEADER)){
            UserSalle userSalle = authService.getCurrentUser();

            List<Long> userGroupIds = userSalle.getGroups().stream()
                    .map(ug -> ug.getGroup().getId())
                    .collect(Collectors.toList());

            eventGroups = eventGroupService.getEventGroupsByEventIdAndGroupIds(eventId, userGroupIds);
        }
        
        return eventGroups.stream()
                .map(EventGroup::getGroupSalle)
                .toList();
    }

    public List<GroupSalle> findAllByStages(List<Integer> stages) {
        return groupRepository.findByStageIn(stages);
    }

    public List<GroupSalle> findAllByStageAndCenter(List<Integer> stages, Long centerId) {
        return groupRepository.findAllByStagesAndCenterId(stages, centerId);
    }

    public GroupSalle saveGroup(GroupSalle group) {
        return groupRepository.save(group);
    }

    public void deleteGroup(Long id) {
        groupRepository.deleteById(id);
    }
}
