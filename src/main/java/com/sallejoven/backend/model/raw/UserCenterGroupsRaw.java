package com.sallejoven.backend.model.raw;

import com.sallejoven.backend.model.entity.Center;
import com.sallejoven.backend.model.entity.GroupSalle;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserCenterGroupsRaw {
    private final Center center;
    private final List<GroupSalle> groups;
    private final Integer userType; // 0..4 (según tu lógica)
}