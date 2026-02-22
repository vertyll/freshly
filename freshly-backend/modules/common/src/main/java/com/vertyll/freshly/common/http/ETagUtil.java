package com.vertyll.freshly.common.http;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ETagUtil {
    private static final String WEAK_PREFIX = "W/\"";
    private static final char STRONG_QUOTE = '"';

    @Nullable
    public static String buildWeakETag(@Nullable Long version) {
        return version != null ? WEAK_PREFIX + version + "\"" : null;
    }

    @Nullable
    public static Long parseIfMatchToVersion(@Nullable String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        String trimmed = ifMatch.trim();
        String raw;
        if (trimmed.startsWith("W/\"") && trimmed.endsWith("\"")) {
            raw = trimmed.substring(2);
        } else {
            raw = trimmed;
        }

        String unquoted = stripQuotes(raw.trim());
        try {
            return Long.parseLong(unquoted);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private static String stripQuotes(String str) {
        if (str.length() >= 2
                && str.charAt(0) == STRONG_QUOTE
                && str.charAt(str.length() - 1) == STRONG_QUOTE) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
}
