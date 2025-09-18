package com.sallejoven.backend.model.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum UserType {
    ADMIN(4),
    PASTORAL_DELEGATE(3),
    GROUP_LEADER(2),
    ANIMATOR(1),
    PARTICIPANT(0);

    private final int code;

    UserType(int code) { this.code = code; }

    /** Código numérico (igual que en Dart). */
    public int toInt() { return code; }

    private static final Map<Integer, UserType> BY_CODE =
            Arrays.stream(values()).collect(Collectors.toMap(UserType::toInt, Function.identity()));

    /** Parsear desde int a UserType. */
    public static UserType fromInt(int code) {
        UserType t = BY_CODE.get(code);
        if (t == null) throw new IllegalArgumentException("Código de userType no válido: " + code);
        return t;
    }

    /** Helper flexible: acepta número o string (nombre o número). */
    public static UserType parse(Object value) {
        if (value instanceof Number) return fromInt(((Number) value).intValue());
        if (value instanceof String s) {
            try { return fromInt(Integer.parseInt(s)); }
            catch (NumberFormatException ignored) { /* cae a valueOf */ }
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        }
        throw new IllegalArgumentException("Valor de userType no reconocido: " + value);
    }
}