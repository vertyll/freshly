package com.vertyll.freshly.translation.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.translation.domain.error.TranslationError;

public enum SupportedLanguage {
    ENGLISH("en"),
    POLISH("pl");

    public static final SupportedLanguage SOURCE = ENGLISH;

    private static final Pattern SEPARATOR = Pattern.compile("[-_]");

    private final String tag;

    SupportedLanguage(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }

    public static SupportedLanguage require(String tag) {
        return of(tag)
            .orElseThrow(() -> new DomainException(TranslationError.UNSUPPORTED_LANGUAGE, Map.of("language", tag)));
    }

    public static Optional<SupportedLanguage> of(String tag) {
        String primary = SEPARATOR.split(tag.strip().toLowerCase(Locale.ROOT), 2)[0];
        return Arrays.stream(values()).filter(language -> language.tag.equals(primary)).findFirst();
    }

    public static List<String> tags() {
        return Arrays.stream(values()).map(SupportedLanguage::tag).toList();
    }
}
