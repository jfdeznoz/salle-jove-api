package com.sallejoven.backend.model.enums;

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
    INVALID_FILE_TYPE("Sólo se permiten archivos PDF", "E019"),
    INVALID_CREDENTIALS("Credenciales incorrectas", "E020"),
    SYSTEM_LOCKED("El sistema está bloqueado para añadir un nuevo usuario", "E021"),
    AWS_CREDENTIALS_NOT_CONFIGURED("No se pueden subir archivos: credenciales de AWS no configuradas", "E022"),
    VITAL_SITUATION_NOT_FOUND("Situación vital no encontrada", "E023"),
    VITAL_SITUATION_SESSION_NOT_FOUND("Sesión de situación vital no encontrada", "E024"),
    WEEKLY_SESSION_NOT_FOUND("Sesión semanal no encontrada", "E025"),
    CENTER_ALREADY_EXISTS("Ya existe un centro con ese nombre y ciudad", "E026"),
    CENTER_HAS_GROUPS("No se puede eliminar un centro que todavía tiene grupos asignados", "E027"),
    SESSION_LOCKED("La sesión no se puede modificar: está archivada o fuera de fecha", "E028"),
    MERGE_STAGE_CONFLICT("Los centros tienen grupos con las mismas etapas", "E029"),
    USER_NOT_DELETED("El usuario no está eliminado", "E030"),
    INVALID_MERGE_TARGET("El usuario origen de la fusión no puede ser el mismo que el usuario base", "E031"),
    EMAIL_IN_USE("Email ya en uso por otro usuario activo", "E032");

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
