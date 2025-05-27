package com.sallejoven.backend.service;

import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.repository.CenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;
    private final GroupService groupService;

    public List<Center> getAllCentersWithGroups() {
        return centerRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<GroupSalle> getGroupsForCenter(Center center) {
        return groupService.findByCenter(center);
    }
}