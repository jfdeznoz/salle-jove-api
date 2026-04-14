package com.sallejoven.backend.model.dto;

import com.sallejoven.backend.model.entity.GroupSalle;

public record GroupResponse(
        java.util.UUID uuid,
        Integer stage,
        java.util.UUID centerUuid,
        String centerName,
        String cityName
) {
    public static GroupResponse from(GroupSalle group) {
        return new GroupResponse(
                group.getUuid(),
                group.getStage(),
                group.getCenter() != null ? group.getCenter().getUuid() : null,
                group.getCenter() != null ? group.getCenter().getName() : null,
                group.getCenter() != null ? group.getCenter().getCity() : null
        );
    }
}
