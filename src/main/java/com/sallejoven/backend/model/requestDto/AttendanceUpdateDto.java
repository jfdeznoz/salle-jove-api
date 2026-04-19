package com.sallejoven.backend.model.requestDto;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.enums.WeeklySessionWarningType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttendanceUpdateDto {

    @NotBlank
    private String userUuid;

    private Integer attends;

    private Boolean justified;

    @Size(max = 300)
    private String justificationReason;

    private WeeklySessionWarningType warningType;

    @Size(max = 500)
    private String warningComment;

    public void validate() {
        if (attends != null && (attends < 0 || attends > 1)) {
            throw new SalleException(ErrorCodes.STATUS_PARTICIPANT_ERROR);
        }
        if (userUuid == null || userUuid.isBlank()) {
            throw new SalleException(ErrorCodes.USER_NOT_FOUND);
        }
        boolean hasWarningType = warningType != null;
        boolean hasWarningComment = warningComment != null && !warningComment.trim().isEmpty();
        if (hasWarningType != hasWarningComment) {
            throw new SalleException(ErrorCodes.WEEKLY_SESSION_WARNING_INVALID);
        }
    }
}
