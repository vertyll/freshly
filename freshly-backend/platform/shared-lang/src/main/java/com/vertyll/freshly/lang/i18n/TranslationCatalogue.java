package com.vertyll.freshly.lang.i18n;

import java.util.Map;

/**
 * The translations one bounded context ships as defaults.
 *
 * <p>
 * Each context contributes one of these; the {@code translation} context collects them
 * at start-up and stores them, knowing none of them. The same shape as
 * {@code PermissionCatalogue} and {@code CacheSpec}, and for the same reason: the platform
 * owns the contract, the contexts own the content, and nothing has to be edited centrally
 * when a module adds a key.
 *
 * <p>
 * What a catalogue declares is the <em>default</em>. An administrator's edit is an
 * override, stored separately, and a redeploy must never overwrite one. That distinction is
 * the whole design: without it, the first time somebody fixes a typo in the UI and the next
 * release quietly reverts it, nobody trusts the feature again.
 *
 * <p>
 * Defaults, by contrast, <em>are</em> overwritten on every start-up, so improving the
 * source text in code reaches everyone who has not overridden it.
 */
public interface TranslationCatalogue {

    /** The context these belong to, e.g. {@code auth}. Used to group the admin screen. */
    String context();

    /**
     * Key to language tag to text.
     *
     * <p>
     * Language tags rather than {@code Locale} because that is what is stored, what an
     * {@code Accept-Language} header carries, and what a client sends back — converting at
     * every boundary buys nothing.
     */
    Map<String, Map<String, String>> defaults();
}
