package com.vertyll.freshly.lang.i18n;

/**
 * The three things a declarative catalogue cannot express.
 *
 * <p>
 * Registration is idempotent: a module states its defaults and they are written, every
 * start-up, from nothing. That covers adding and changing text, which is almost everything.
 *
 * <p>
 * It cannot cover an operation that <em>transforms</em> what is already stored. Renaming
 * a key would leave the old document behind holding somebody's override; retiring one would
 * leave it forever; moving one to another module is refused outright, because a key belongs
 * to the context that declared it. Each of those has to happen once, in order, and be
 * remembered — which is what a migration is.
 *
 * <p>
 * Deliberately three verbs and no more. A fourth that writes text would put the same
 * sentence in two places and start the drift the defaults/overrides split exists to prevent.
 */
public interface TranslationMigrationOperations {

    /**
     * Moves a key, carrying its overrides to the new name.
     *
     * <p>
     * An override already on the target wins: the target is the name in use, so what is
     * stored under it is the more recent decision. Defaults are not carried — the owning
     * module writes those at the next registration, moments later.
     *
     * <p>
     * A no-op when the old key is gone, so a migration that has run on one instance does
     * not fail on the next.
     */
    void rename(String fromKey, String toKey);

    /** Removes a key and its overrides. For text a module no longer ships and will not again. */
    void retire(String key);

    /** Hands a key to another context, for a concept that moved between modules. */
    void reassign(String key, String toContext);
}
