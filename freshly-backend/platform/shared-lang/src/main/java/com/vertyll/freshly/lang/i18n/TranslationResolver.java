package com.vertyll.freshly.lang.i18n;

import java.util.Optional;

/**
 * Resolves one key in one language.
 *
 * <p>
 * The SPI the platform uses to reach the {@code translation} context without depending on
 * it — the same inversion as {@code PermissionEvaluator}. {@code shared-web}'s
 * {@code MessageResolver} calls this; {@code translation-infrastructure} supplies the bean.
 *
 * <p>
 * Returns empty rather than a fallback, so the caller decides. {@code MessageResolver}
 * falls back to the key, which keeps a missing translation from becoming a 500 while staying
 * visible enough to notice.
 */
public interface TranslationResolver {

    Optional<String> resolve(String key, String languageTag);
}
