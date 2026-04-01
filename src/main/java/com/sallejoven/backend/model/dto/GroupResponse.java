package com.sallejoven.backend.model.dto;

import com.sallejoven.backend.model.entity.GroupSalle;

public record GroupResponse(
        Long id,
        Integer stage,
        Long centerId,
        String centerName,
        String cityName
) {
    public static GroupResponse from(GroupSalle group) {
        return new GroupResponse(
                group.getId(),
                group.getStage(),
                group.getCenter() != null ? group.getCenter().getId() : null,
                group.getCenter() != null ? group.getCenter().getName() : null,
                group.getCenter() != null ? group.getCenter().getCity() : null
        );
    }
}
