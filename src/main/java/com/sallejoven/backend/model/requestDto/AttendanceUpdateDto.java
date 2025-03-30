package com.sallejoven.backend.model.requestDto;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.types.ErrorCodes;

import lombok.Data;

@Data
public class AttendanceUpdateDto {
    private Long userId;
    private Integer attends;

    public void validate() throws SalleException {
        if (attends < 0 || attends > 1) {
            throw new SalleException(ErrorCodes.STATUS_PARTICIPANT_ERROR);
        }
    }
}
