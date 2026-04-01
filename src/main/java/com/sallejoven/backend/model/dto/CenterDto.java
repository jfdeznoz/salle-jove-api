package com.sallejoven.backend.model.dto;

import java.util.List;

public record CenterDto(
    Long id,
    String name,
    String city,
    List<GroupDto> groups
) {}
