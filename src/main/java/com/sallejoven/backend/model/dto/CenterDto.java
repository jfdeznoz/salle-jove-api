package com.sallejoven.backend.model.dto;

import java.util.List;

public record CenterDto(
    java.util.UUID uuid,
    String name,
    String city,
    List<GroupDto> groups
) {}
