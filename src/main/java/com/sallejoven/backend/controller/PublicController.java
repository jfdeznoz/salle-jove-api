package com.sallejoven.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import com.sallejoven.backend.mapper.CenterMapper;
import com.sallejoven.backend.mapper.GroupMapper;
import com.sallejoven.backend.model.dto.CenterDto;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.service.CenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final CenterService centerService;
    private final CenterMapper centerMapper;
    private final GroupMapper groupMapper;

    @GetMapping("/info")
    public ResponseEntity<String> getPublicInfo() {
        return ResponseEntity.ok("This is a public endpoint accessible by anyone.(2)");
    }

    @GetMapping("/centers")
    public ResponseEntity<List<CenterDto>> getAllCentersWithGroups() {
        List<Center> centers = centerService.getAllCentersWithGroups();

        List<CenterDto> result = centers.stream()
                .map(center -> {
                    List<GroupSalle> groups = centerService.getGroupsForCenter(center);
                    List<GroupDto> groupDtos = groups.stream()
                            .map(groupMapper::toGroupDto)
                            .toList();
                    return centerMapper.toCenterDtoWithGroups(center, groupDtos);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

}
