package com.sallejoven.backend.model.types;

public enum ErrorCodes {

    USER_NOT_FOUND("Usuario no encontrado", "E001"),
    EVENT_NOT_FOUND("Evento no encontrado", "E002"),
    STATUS_PARTICIPANT_ERROR("El estado del participante no es correcto", "E003"),
    GROUP_NOT_FOUND("Grupo no encontrado", "E004"),
    USER_GROUP_NOT_FOUND("El usuario no está asignado a ningún grupo", "E005"),
    BLOCK_EVENT_ERROR_ADMIN("Solo un admin puede bloquear eventos generales", "E006"),
    BLOCK_EVENT_ERROR("No tienes permiso para bloquear este evento local", "E007"),
    CENTER_NOT_FOUND("Centro no encontrado", "E008"),
    USER_GROUP_NOT_ASSIGNED("El usuario no pertenece al grupo origen", "E009"),
    EVENT_USER_NOT_FOUND("No se ha encontrado que el usuario vaya a dicho evento", "E010"),
    PROMOTION_TARGET_GROUP_NOT_FOUND("No se ha encontrado el grupo por grupo y etapa", "E011"),
    ACADEMIC_STATE_NOT_INITIALIZED("Estado año académico no inicializado", "E012"),
    USER_TYPE_CENTER_NOT_VALID("El tipo de usuario para un centro debe ser 2 o 3", "E013"),
    USER_TYPE_CENTER_EXISTS("Ya existe este usuario para el centro", "E014"),
    USER_CENTER_NOT_FOUND("No se ha encontrado el user center", "E015"),
    USER_TYPE_NOT_VALID("El tipo de usuario no es correcto", "E016"),
    EMAIL_ALREADY_EXISTS("Ya existe un usuario con este email", "E017"),
    DNI_ALREADY_EXISTS("Ya existe un usuario con este dni", "E018"),
    INVALID_FILE_TYPE("Sólo se permiten archivos PDF", "E019");

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
