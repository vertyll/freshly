package com.vertyll.freshly.web.i18n;

import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.i18n.IcuMessages;
import com.vertyll.freshly.lang.i18n.TranslationResolver;

import lombok.RequiredArgsConstructor;

/**
 * Resolves a message key against the request's locale.
 *
 * <p>
 * Reads from the {@code translation} context through the {@link TranslationResolver} SPI
 * rather than from {@code messages_*.properties}, so the text is data an administrator can
 * correct rather than a build artifact that needs a deployment to change.
 *
 * <p>
 * The platform still does not depend on that module: it names the interface,
 * {@code translation-infrastructure} supplies the bean. The same inversion as
 * {@code PermissionEvaluator}.
 */
@Component
@RequiredArgsConstructor
public class MessageResolver {
    private final TranslationResolver translations;

    /**
     * Resolves a key with no arguments.
     *
     * <p>
     * A key with no text renders as the key itself — never as another language's text,
     * and never as an exception. {@code error.user.notFound} on a screen says exactly what
     * is missing and where to add it; English served to a Polish reader looks like a choice
     * somebody made, and a 500 on the error path would bury the error it was reporting.
     */
    public String resolve(String key) {
        return resolve(key, Map.of());
    }

    /**
     * Resolves and interpolates by argument name.
     *
     * <p>
     * Named rather than positional because the names are what a translator sees. A key
     * whose text reads <code>"od {min} do {max} znaków"</code> can be edited by somebody who
     * never opens the code; <code>{0}</code> and <code>{1}</code> cannot, and getting them
     * the wrong way round produces a sentence that is wrong rather than one that is broken.
     */
    public String resolve(String key, Map<String, Object> arguments) {
        String languageTag = LocaleContextHolder.getLocale().getLanguage();

        String pattern = translations.resolve(key, languageTag).orElse(key);
        return arguments.isEmpty() ? pattern : IcuMessages.formatNamed(languageTag, pattern, arguments);
    }
}
