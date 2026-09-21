package com.vertyll.freshly.translation.domain.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.translation.domain.error.TranslationError;

import static java.util.Objects.requireNonNull;

public final class TranslationKey {
    private static final String KEY_NULL = "Translation key cannot be null";
    private static final String CONTEXT_NULL = "Owning context cannot be null";
    private static final String KEY = "key";
    private static final String LANGUAGE = "language";

    private static final Pattern SEGMENT_SEPARATOR = Pattern.compile("\\.");
    private static final Pattern FIRST_SEGMENT = Pattern.compile("[a-z][a-zA-Z0-9]*");
    private static final Pattern SEGMENT = Pattern.compile("[a-zA-Z0-9]+");

    private final String key;
    private String context;
    private boolean declared;

    private final Map<String, String> defaults;
    private final Map<String, LanguageOverride> overrides;

    @Nullable private final Long version;

    private TranslationKey(
        String key,
        String context,
        boolean declared,
        Map<String, String> defaults,
        Map<String, LanguageOverride> overrides,
        @Nullable Long version
    ) {
        this.key = requireValidKey(key);
        this.context = requireNonNull(context, CONTEXT_NULL);
        this.declared = declared;
        this.defaults = new LinkedHashMap<>(requireNonNull(defaults, "Defaults cannot be null"));
        this.overrides = new LinkedHashMap<>(requireNonNull(overrides, "Overrides cannot be null"));
        this.version = version;
    }

    public static TranslationKey declare(
        String key,
        String context,
        Map<String, String> defaults,
        MessageGrammar grammar
    ) {
        requireRenderable(key, defaults, grammar);
        return new TranslationKey(key, context, true, defaults, Map.of(), null);
    }

    public static TranslationKey reconstitute(
        String key,
        String context,
        boolean declared,
        Map<String, String> defaults,
        Map<String, LanguageOverride> overrides,
        @Nullable Long version
    ) {
        return new TranslationKey(key, context, declared, defaults, overrides, version);
    }

    public void refreshDefaults(String owningContext, Map<String, String> newDefaults, MessageGrammar grammar) {
        if (!context.equals(owningContext)) {
            throw new DomainException(
                TranslationError.KEY_OWNED_BY_ANOTHER_CONTEXT,
                Map.of(KEY, key, "owner", context, "claimant", owningContext)
            );
        }
        requireRenderable(key, newDefaults, grammar);

        defaults.clear();
        defaults.putAll(newDefaults);
        declared = true;
    }

    public void markOrphaned() {
        declared = false;
    }

    public boolean isOrphanWithoutEdits() {
        return !declared && overrides.isEmpty();
    }

    public void reassignTo(String newContext) {
        context = requireNonNull(newContext, CONTEXT_NULL);
    }

    public void adoptOverridesFrom(TranslationKey other) {
        other.overrides.forEach(overrides::putIfAbsent);
    }

    public void override(String languageTag, String text, String author, MessageGrammar grammar) {
        SupportedLanguage.require(languageTag);

        if (text.isBlank()) {
            throw new DomainException(TranslationError.BLANK_OVERRIDE, Map.of(KEY, key));
        }
        grammar.rejectionReason(languageTag, text).ifPresent(reason -> {
            throw new DomainException(
                TranslationError.INVALID_PATTERN,
                Map.of(KEY, key, LANGUAGE, languageTag, "reason", reason)
            );
        });
        requireSamePlaceholders(languageTag, text, grammar);

        overrides.put(languageTag, new LanguageOverride(text, defaults.get(languageTag), author, Instant.now()));
    }

    public void clearOverride(String languageTag) {
        SupportedLanguage.require(languageTag);

        if (overrides.remove(languageTag) == null) {
            throw new DomainException(TranslationError.OVERRIDE_NOT_FOUND, Map.of(KEY, key, LANGUAGE, languageTag));
        }
    }

    public Optional<String> resolve(String languageTag) {
        LanguageOverride override = overrides.get(languageTag);
        if (override != null) {
            return Optional.of(override.text());
        }
        return Optional.ofNullable(defaults.get(languageTag));
    }

    public Map<String, LanguageOverride> staleOverrides() {
        Map<String, LanguageOverride> stale = new LinkedHashMap<>();
        overrides.forEach((language, override) -> {
            String sourceDefault = override.sourceDefault();
            if (sourceDefault != null && !sourceDefault.equals(defaults.get(language))) {
                stale.put(language, override);
            }
        });
        return Map.copyOf(stale);
    }

    public String key() {
        return key;
    }

    public String context() {
        return context;
    }

    public boolean declared() {
        return declared;
    }

    public Map<String, String> defaults() {
        return Map.copyOf(defaults);
    }

    public Map<String, LanguageOverride> overrides() {
        return Map.copyOf(overrides);
    }

    @Nullable public Long version() {
        return version;
    }

    public record LanguageOverride(String text, @Nullable String sourceDefault, String author, Instant at) {
        public LanguageOverride {
            requireNonNull(text, "Override text cannot be null");
            requireNonNull(author, "Override author cannot be null");
            requireNonNull(at, "Override timestamp cannot be null");
        }
    }

    private void requireSamePlaceholders(String languageTag, String text, MessageGrammar grammar) {
        String currentDefault = defaults.get(languageTag);
        if (currentDefault == null) {
            return;
        }

        Set<String> expected = grammar.placeholdersOf(languageTag, currentDefault);
        Set<String> actual = grammar.placeholdersOf(languageTag, text);
        if (expected.equals(actual)) {
            return;
        }

        throw new DomainException(
            TranslationError.PLACEHOLDER_MISMATCH,
            Map.of(
                KEY,
                key,
                LANGUAGE,
                languageTag,
                "expected",
                new TreeSet<>(expected).toString(),
                "actual",
                new TreeSet<>(actual).toString()
            )
        );
    }

    private static void requireRenderable(String key, Map<String, String> texts, MessageGrammar grammar) {
        texts.forEach((languageTag, text) -> grammar.rejectionReason(languageTag, text).ifPresent(reason -> {
            throw new DomainException(
                TranslationError.INVALID_PATTERN,
                Map.of(KEY, key, LANGUAGE, languageTag, "reason", reason)
            );
        }));
    }

    private static String requireValidKey(String candidate) {
        String value = requireNonNull(candidate, KEY_NULL).trim();
        if (!isValidKey(value)) {
            throw new DomainException(TranslationError.INVALID_KEY_FORMAT, Map.of(KEY, value));
        }
        return value;
    }

    private static boolean isValidKey(String value) {
        String[] segments = SEGMENT_SEPARATOR.split(value, -1);
        return segments.length > 1 && FIRST_SEGMENT.matcher(segments[0]).matches()
                && Arrays.stream(segments, 1, segments.length).allMatch(segment -> SEGMENT.matcher(segment).matches());
    }
}
