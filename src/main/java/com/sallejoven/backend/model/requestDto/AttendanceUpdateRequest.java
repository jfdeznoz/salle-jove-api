package com.sallejoven.backend.model.requestDto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceUpdateRequest {
    private List<AttendanceUpdateDto> participants;
}

