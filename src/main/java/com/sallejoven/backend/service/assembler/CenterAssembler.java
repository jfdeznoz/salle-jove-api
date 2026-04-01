package com.sallejoven.backend.service.assembler;

import com.sallejoven.backend.mapper.CenterMapper;
import com.sallejoven.backend.mapper.GroupMapper;
import com.sallejoven.backend.model.dto.CenterDto;
import com.sallejoven.backend.model.dto.GroupDto;
import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CenterAssembler {

    private final GroupRepository groupRepository;
    private final CenterMapper centerMapper;
    private final GroupMapper groupMapper;

    public List<CenterDto> toCenterDtosWithGroups(List<Center> centers) {
        List<Long> centerIds = centers.stream().map(Center::getId).toList();
        List<GroupSalle> allGroups = groupRepository.findByCenterIdIn(centerIds);

        Map<Long, List<GroupSalle>> groupsByCenter = allGroups.stream()
                .collect(Collectors.groupingBy(g -> g.getCenter().getId()));

        return centers.stream().map(c -> {
            List<GroupSalle> groups = groupsByCenter.getOrDefault(c.getId(), List.of());
            List<GroupDto> groupDtos = groups.stream().map(groupMapper::toGroupDto).toList();
            return centerMapper.toCenterDtoWithGroups(c, groupDtos);
        }).toList();
    }
}
