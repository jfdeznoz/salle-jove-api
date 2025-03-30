package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public List<GroupSalle> findAllByEvent(Long eventId) throws SalleException {
        List<Role> roles = authService.getCurrentUserRoles();
        List<EventGroup> eventGroups = new ArrayList<>();

        if(roles.contains(Role.ADMIN)){
            eventGroups = eventGroupService.getEventGroupsByEventId(eventId);
        }else if(roles.contains(Role.PASTORAL_DELEGATE) || roles.contains(Role.GROUP_LEADER)){
            UserSalle userSalle = authService.getCurrentUser();
            Set<GroupSalle> userGroups = userSalle.getGroups();

            List<Long> userGroupIds = userGroups.stream()
                .map(GroupSalle::getId)
                .toList();

            eventGroups = eventGroupService.getEventGroupsByEventIdAndGroupIds(eventId, userGroupIds);
        }else  if(roles.contains(Role.ANIMATOR)){
            UserSalle userSalle = authService.getCurrentUser();
            Set<GroupSalle> userGroups = userSalle.getGroups();

            List<Long> userGroupIds = userGroups.stream()
                .map(GroupSalle::getId)
                .toList();

            eventGroups = eventGroupService.getEventGroupsByEventIdAndGroupIds(eventId, userGroupIds);
        }
        
        return eventGroups.stream()
                .map(EventGroup::getGroupSalle)
                .toList();
    }

    public List<GroupSalle> findAllByStage(List<Integer> stages) {
        return groupRepository.findByStageIn(stages);
    }

    public GroupSalle saveGroup(GroupSalle group) {
        return groupRepository.save(group);
    }

    public void deleteGroup(Long id) {
        groupRepository.deleteById(id);
    }
}
