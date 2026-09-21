package com.vertyll.freshly.lang.i18n;

/**
 * One translation change that has to happen once rather than be recomputed.
 *
 * <p>
 * Declared by the module that owns the keys, like its {@link TranslationCatalogue}, and
 * discovered the same way. Each runs in its own transaction and its {@link #id()} is
 * recorded, so a migration that has run is never run again and one that failed is retried
 * on the next start-up.
 *
 * <p>
 * An id is permanent. Change it and the migration runs a second time; reuse one and it
 * never runs at all. Dating it and naming what it does — {@code
 * "2026-09-auth-rename-token-expired"} — makes both mistakes visible in review.
 */
public interface TranslationMigration {

    /** Permanent and unique across the application. */
    String id();

    /** The declaring module, for the log line and for reading the list in order. */
    String context();

    void apply(TranslationMigrationOperations operations);
}
