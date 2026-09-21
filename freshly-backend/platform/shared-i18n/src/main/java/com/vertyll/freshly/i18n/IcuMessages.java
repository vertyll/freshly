package com.vertyll.freshly.i18n;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.ibm.icu.text.MessageFormat;

/**
 * Checks and renders ICU message patterns.
 *
 * <p>
 * ICU rather than {@code java.text.MessageFormat} because of plurals. Polish needs
 * {@code one}, {@code few} and {@code many}, selected by a rule on the last digits —
 * "1 stacja", "2 stacje", "5 stacji", "22 stacje". The JDK's {@code ChoiceFormat} picks a
 * branch by numeric threshold, so writing that rule out by hand is both unreadable and
 * wrong for 22. ICU carries the CLDR rules for every language it knows.
 *
 * <p>
 * Two further differences matter in practice. Arguments can be named, so a translator
 * sees <code>{min}</code> and <code>{max}</code> rather than <code>{0}</code> and
 * <code>{1}</code> and does not have to guess which is which. And an apostrophe only
 * escapes when it precedes a brace, so {@code "don't"} survives — under the JDK formatter
 * it silently renders as {@code "dont"}, in exactly the languages that use apostrophes.
 *
 * <p>
 * Static because a pattern is checked in three places that share nothing else: a module
 * declaring its defaults, an administrator saving an override, and a spreadsheet import
 * reporting what it refused.
 */
public final class IcuMessages {

    private IcuMessages() {
    }

    /**
     * The reason this pattern will not compile, or empty when it will.
     *
     * <p>
     * Returns the reason rather than throwing because the import path shows it to whoever
     * uploaded the file, one row at a time, and has to keep going.
     */
    public static Optional<String> validate(String languageTag, String pattern) {
        try {
            new MessageFormat(pattern, localeOf(languageTag));
            return Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.of(reasonOf(malformed));
        }
    }

    /**
     * The placeholder names a pattern uses, positional ones included as {@code "0"}, {@code "1"}.
     *
     * @throws IllegalArgumentException for a pattern that does not compile. Callers validate
     *     first; reaching this with a malformed pattern is a caller that skipped
     *     {@link #validate}, and an empty set would pass it off as a pattern with no
     *     placeholders.
     */
    public static Set<String> argumentsOf(String languageTag, String pattern) {
        return Set.copyOf(new MessageFormat(pattern, localeOf(languageTag)).getArgumentNames());
    }

    /**
     * Renders with named arguments — {@code {count}}, {@code {username}}.
     *
     * <p>
     * Named separately from {@link #formatPositional}: same arity, and neither
     * parameter type is a subtype of the other, so a single overloaded name would resolve
     * a {@code Map<String, String>} to the varargs form and render the whole map as
     * argument zero. Silently, and only in the output.
     *
     * <p>
     * Neither form catches ICU's {@code IllegalArgumentException}. Every stored pattern
     * was validated on the way in, so one that fails here is corrupt data or an argument of
     * the wrong type for its {@code plural} or {@code number} — both defects, and returning
     * the raw pattern would put the curly braces on a client's screen instead of in a log.
     */
    public static String formatNamed(String languageTag, String pattern, Map<String, Object> arguments) {
        return new MessageFormat(pattern, localeOf(languageTag)).format(arguments);
    }

    /**
     * Renders with positional arguments, which is what Thymeleaf's {@code #{key(a, b)}} passes.
     *
     * <p>
     * An argument may be null: a template can pass a variable that has no value, and ICU
     * renders it as {@code "null"} rather than failing the whole message.
     */
    public static String formatPositional(String languageTag, String pattern, @Nullable Object... arguments) {
        return new MessageFormat(pattern, localeOf(languageTag)).format(arguments);
    }

    private static Locale localeOf(String languageTag) {
        return Locale.forLanguageTag(languageTag);
    }

    private static String reasonOf(IllegalArgumentException malformed) {
        String message = malformed.getMessage();
        return message == null ? malformed.toString() : message;
    }
}
