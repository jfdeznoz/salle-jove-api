package com.sallejoven.backend.model.requestDto;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.enums.ErrorCodes;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceUpdateDto {
    @NotNull
    private Long userId;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer attends;

    public void validate() {
        if (attends < 0 || attends > 1) {
            throw new SalleException(ErrorCodes.STATUS_PARTICIPANT_ERROR);
        }
    }
}
