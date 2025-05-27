package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.CenterDto;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.types.ErrorCodes;
import com.sallejoven.backend.service.CenterService;
import com.sallejoven.backend.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/centers")
@RequiredArgsConstructor
public class CenterController {

    private final CenterService centerService;
    private final GroupService groupService;

    /**
     * Devuelve todos los centros con sus grupos
     */
    @GetMapping
    public ResponseEntity<List<CenterDto>> listAll() {
        List<Center> centers = centerService.getAllCentersWithGroups();
        List<CenterDto> dtos = centers.stream().map(center -> {
            var groups = centerService.getGroupsForCenter(center);
            var groupDtos = groups.stream().map(g ->
                GroupDto.builder()
                        .groupId(Math.toIntExact(g.getId()))
                        .centerId(Math.toIntExact(center.getId()))
                        .stage(g.getStage())
                        .centerName(center.getName())
                        .cityName(center.getCity())
                        .build()
            ).collect(Collectors.toList());
            return CenterDto.builder()
                    .id(center.getId())
                    .name(center.getName())
                    .groups(groupDtos)
                    .build();
        }).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Devuelve únicamente los grupos de un centro
     */
    @GetMapping("/{centerId}/groups")
    public ResponseEntity<List<GroupDto>> listGroups(
            @PathVariable Long centerId) throws SalleException {

        // Primero recuperamos el centro y comprobamos que existe
        Center center = centerService.getAllCentersWithGroups().stream()
                .filter(c -> c.getId().equals(centerId))
                .findFirst()
                .orElseThrow(() -> new SalleException(ErrorCodes.CENTER_NOT_FOUND));

        List<GroupSalle> groups = groupService.findByCenter(center);
        List<GroupDto> dtos = groups.stream().map(g ->
            GroupDto.builder()
                    .groupId(Math.toIntExact(g.getId()))
                    .centerId(Math.toIntExact(center.getId()))
                    .stage(g.getStage())
                    .centerName(center.getName())
                    .cityName(center.getCity())
                    .build()
        ).toList();

        return ResponseEntity.ok(dtos);
    }
}