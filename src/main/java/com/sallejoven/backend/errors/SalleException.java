package com.sallejoven.backend.errors;

import com.sallejoven.backend.model.types.ErrorCodes;

public class SalleException extends Exception{

    private final String errorCode;
    private final String additionalInfo;

    public SalleException(ErrorCodes errorCodes) {
        super(errorCodes.getMessage());
        this.errorCode = errorCodes.getErrorCode();
        this.additionalInfo = null;
    }

    public SalleException(ErrorCodes errorCodes, String additionalInfo) {
        super(errorCodes.getMessage());
        this.errorCode = errorCodes.getErrorCode();
        this.additionalInfo = additionalInfo;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }
}
