package com.sallejoven.backend.utils;

import java.util.Optional;
import java.util.UUID;

public final class ReferenceParser {

    private ReferenceParser() {
    }

    public static Optional<UUID> asUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static UUID requireUuid(String value) {
        return asUuid(value).orElseThrow(() -> new IllegalArgumentException("Invalid UUID reference: " + value));
    }
}
