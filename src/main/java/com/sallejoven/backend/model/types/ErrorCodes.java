package com.sallejoven.backend.model.types;

public enum ErrorCodes {

    USER_NOT_FOUND("Usuario no encontrado", "E001"),
    EVENT_NOT_FOUND("Evento no encontrado", "E002"),
    STATUS_PARTICIPANT_ERROR("El estado del participante no es correcto", "E003");


    private final String message;
    private final String errorCode;

    ErrorCodes(String message, String errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }

    public String getMessage(){
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
