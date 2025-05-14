package com.sallejoven.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sallejoven.backend.model.dto.CenterDto;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.service.CenterService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final CenterService centerService;

    @GetMapping("/info")
    public ResponseEntity<String> getPublicInfo() {
        return ResponseEntity.ok("This is a public endpoint accessible by anyone.");
    }

    @GetMapping("/centers")
    public ResponseEntity<List<CenterDto>> getAllCentersWithGroups() {
        List<Center> centers = centerService.getAllCentersWithGroups();

        List<CenterDto> result = centers.stream().map(center -> {
            List<GroupSalle> groups = centerService.getGroupsForCenter(center);

            List<GroupDto> groupDtos = groups.stream().map(group ->
                GroupDto.builder()
                    .groupId(Math.toIntExact(group.getId()))
                    .stage(group.getStage())
                    .centerName(center.getName())
                    .cityName(center.getCity())
                    .build()
            ).collect(Collectors.toList());

            return CenterDto.builder()
                    .id(center.getId())
                    .name(center.getName())
                    .groups(groupDtos)
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}