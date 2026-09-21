package com.vertyll.freshly.translation.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.translation.domain.model.TranslationKey;

public record TranslationEntry(
    String key,
    String context,
    Map<String, LanguageValue> values,
    boolean hasStaleOverride,
    @Nullable Long version
) {

    public TranslationEntry {
        values = Map.copyOf(values);
    }

    public record LanguageValue(
        @Nullable String defaultText,
        @Nullable String overrideText,
        @Nullable String author,
        @Nullable Instant overriddenAt,
        boolean stale
    ) {
        static LanguageValue of(
            @Nullable String defaultText,
            TranslationKey.@Nullable LanguageOverride override,
            boolean stale
        ) {
            return override == null ? new LanguageValue(defaultText, null, null, null, stale)
                    : new LanguageValue(defaultText, override.text(), override.author(), override.at(), stale);
        }
    }

    public static TranslationEntry from(TranslationKey key, List<String> languages) {
        Map<String, TranslationKey.LanguageOverride> stale = key.staleOverrides();
        Map<String, TranslationKey.LanguageOverride> overrides = key.overrides();
        Map<String, String> defaults = key.defaults();
        Map<String, LanguageValue> values = languages.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    Function.identity(),
                    language -> LanguageValue
                        .of(defaults.get(language), overrides.get(language), stale.containsKey(language))
                )
            );

        return new TranslationEntry(key.key(), key.context(), values, !stale.isEmpty(), key.version());
    }
}
