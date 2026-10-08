package com.vertyll.freshly.web.i18n;

import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.i18n.IcuMessages;
import com.vertyll.freshly.lang.i18n.TranslationResolver;

import lombok.RequiredArgsConstructor;

/**
 * A {@code MessageSource} backed by the translation store.
 *
 * <p>
 * There are no {@code messages_*.properties} in this application. Registered as the primary
 * {@code MessageSource}, this means anything reaching for one — a Spring internal, a
 * library — gets the same text an administrator edits, rather than a second source of
 * truth nobody remembers exists.
 *
 * <p>
 * Implements the interface rather than extending {@code AbstractMessageSource}, whose
 * {@code resolveCode} hook is typed to return a {@code java.text.MessageFormat}. Patterns
 * here are ICU, which is a different class and not a subtype, so the base class has nowhere
 * to put one. What it offered — a format cache and a code-fallback loop — is a few lines
 * below, and the store is already cached in front of this.
 *
 * <p>
 * Application code should still use {@link MessageResolver}: it is one dependency instead
 * of a locale plus a key plus a fallback decision at each call site, and it takes named
 * arguments, which is what the translations are written against.
 */
@Component("messageSource")
@RequiredArgsConstructor
public class TranslationMessageSource implements MessageSource {
    private final TranslationResolver translations;

    @Override
    @Nullable
    public String getMessage(
        String code,
        @Nullable Object @Nullable [] args,
        @Nullable String defaultMessage,
        @Nullable Locale locale
    ) {
        String rendered = render(code, args, locale);
        return rendered == null ? defaultMessage : rendered;
    }

    @Override
    public String getMessage(String code, @Nullable Object @Nullable [] args, @Nullable Locale locale) {
        String rendered = render(code, args, locale);
        if (rendered == null) {
            throw new NoSuchMessageException(code, localeOf(locale));
        }
        return rendered;
    }

    /**
     * Tries each code in turn, which is how Spring's own validation messages degrade.
     *
     * <p>
     * A resolvable carries codes from most to least specific, and the contract is that the
     * first one with text wins.
     */
    @Override
    public String getMessage(MessageSourceResolvable resolvable, @Nullable Locale locale) {
        String[] codes = resolvable.getCodes();
        if (codes != null) {
            for (String code : codes) {
                String rendered = render(code, resolvable.getArguments(), locale);
                if (rendered != null) {
                    return rendered;
                }
            }
        }

        String defaultMessage = resolvable.getDefaultMessage();
        if (defaultMessage != null) {
            return defaultMessage;
        }
        throw new NoSuchMessageException(
            codes == null || codes.length == 0 ? "" : codes[codes.length - 1],
            localeOf(locale)
        );
    }

    @Nullable
    private String render(String code, @Nullable Object @Nullable [] args, @Nullable Locale locale) {
        String languageTag = localeOf(locale).getLanguage();
        String pattern = translations.resolve(code, languageTag).orElse(null);

        if (pattern == null) {
            return null;
        }
        return args == null || args.length == 0 ? pattern : IcuMessages.formatPositional(languageTag, pattern, args);
    }

    /**
     * The request's locale when the caller passes none, which the {@code MessageSource}
     * contract allows. {@code LocaleContextHolder} rather than {@code Locale.getDefault()}:
     * the JVM default is the server's language, not the reader's.
     */
    private static Locale localeOf(@Nullable Locale locale) {
        return locale == null ? LocaleContextHolder.getLocale() : locale;
    }
}
