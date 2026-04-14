package com.sallejoven.backend.model.requestDto;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.enums.ErrorCodes;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttendanceUpdateDto {

    @NotBlank
    private String userUuid;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer attends;

    private Boolean justified;

    @Size(max = 300)
    private String justificationReason;

    public void validate() {
        if (attends < 0 || attends > 1) {
            throw new SalleException(ErrorCodes.STATUS_PARTICIPANT_ERROR);
        }
        if (userUuid == null || userUuid.isBlank()) {
            throw new SalleException(ErrorCodes.USER_NOT_FOUND);
        }
    }
}
