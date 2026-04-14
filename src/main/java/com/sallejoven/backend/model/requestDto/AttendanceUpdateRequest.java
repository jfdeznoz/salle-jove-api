package com.sallejoven.backend.model.requestDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceUpdateRequest {
    @NotEmpty
    @Valid
    private List<AttendanceUpdateDto> participants;
}
