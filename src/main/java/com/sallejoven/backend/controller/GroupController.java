package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.service.GroupService;
import com.sallejoven.backend.utils.SalleConverters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final SalleConverters salleConverters;


    @Autowired
    public GroupController(GroupService groupService, SalleConverters salleConverters) {
        this.groupService = groupService;
        this.salleConverters = salleConverters;
    }

   @GetMapping("/")
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        List<GroupSalle> groupList = groupService.findAll();
        List<GroupDto> groupDtos = groupList.stream()
                                            .map(salleConverters::groupToDto)
                                            .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<GroupDto>> getAllGroupsByEvent(@PathVariable Long eventId) throws SalleException {
        List<GroupSalle> groupList = groupService.findAllByEvent(eventId);
        List<GroupDto> groupDtos = groupList.stream()
                                            .map(salleConverters::groupToDto)
                                            .collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupSalle> getGroupById(@PathVariable Long id) {
        Optional<GroupSalle> group = groupService.findById(id);
        return group.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    public ResponseEntity<GroupSalle> createGroup(@RequestBody GroupSalle group) {
        return ResponseEntity.ok(groupService.saveGroup(group));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupSalle> updateGroup(@PathVariable Long id, @RequestBody GroupSalle groupDetails) {
        Optional<GroupSalle> group = groupService.findById(id);
        if (group.isPresent()) {
            GroupSalle existingGroup = group.get();
            existingGroup.setStage(groupDetails.getStage());
            return ResponseEntity.ok(groupService.saveGroup(existingGroup));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        if (groupService.findById(id).isPresent()) {
            groupService.deleteGroup(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}