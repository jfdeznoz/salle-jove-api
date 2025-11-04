// src/main/java/.../model/dto/GlobalStateDto.java
package com.sallejoven.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GlobalStateDto {
    private boolean locked;
    private List<UserPendingDto> pendings;
}
