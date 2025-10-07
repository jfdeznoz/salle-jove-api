package com.sallejoven.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.UserSelfDto;
import com.sallejoven.backend.model.entity.UserPending;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.requestDto.UserSalleRequest;
import com.sallejoven.backend.service.UserService;
import com.sallejoven.backend.utils.SalleConverters;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final UserService userService;
    private final SalleConverters salleConverters;

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
                    .centerId(Math.toIntExact(center.getId()))
                    .centerName(center.getName())
                    .cityName(center.getCity())
                    .build()
            ).collect(Collectors.toList());

            return CenterDto.builder()
                    .id(center.getId())
                    .name(center.getName())
                    .city(center.getCity())
                    .groups(groupDtos)
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

}