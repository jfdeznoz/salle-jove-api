package com.sallejoven.backend.model.types;

public enum ErrorCodes {

    USER_NOT_FOUND("Usuario no encontrado", "E001"),
    EVENT_NOT_FOUND("Evento no encontrado", "E002"),
    STATUS_PARTICIPANT_ERROR("El estado del participante no es correcto", "E003"),
    GROUP_NOT_FOUND("Grupo no encontrado", "E004"),
    USER_GROUP_NOT_FOUND("El usuario no está asignado a ningún grupo", "E005"),
    BLOCK_EVENT_ERROR_ADMIN("Solo un admin puede bloquear eventos generales", "E006"),
    BLOCK_EVENT_ERROR("No tienes permiso para bloquear este evento local", "E007"),
    CENTER_NOT_FOUND("Centro no encontrado", "E008");



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
