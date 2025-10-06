package com.sallejoven.backend.utils;

import java.text.Normalizer;

public final class TextNormalizeUtils {

    private TextNormalizeUtils() {}

    public static String toLowerNoAccents(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase(java.util.Locale.ROOT);
        String nfd = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }
}
