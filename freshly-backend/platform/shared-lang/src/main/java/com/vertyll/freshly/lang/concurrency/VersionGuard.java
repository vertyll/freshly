package com.vertyll.freshly.lang.concurrency;

import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.lang.error.DomainException;

/**
 * The optimistic-locking check, expressed where the rule belongs.
 *
 * <p>
 * A null {@code expected} means the caller sent no {@code If-Match} header and
 * has therefore asked for no check. That is a deliberate allowance rather than an
 * oversight, and it is worth a test of its own: making the header mandatory would
 * refuse every client that writes without reading back first.
 */
public final class VersionGuard {

    private VersionGuard() {
    }

    public static void requireMatch(
        @Nullable Long current,
        @Nullable Long expected,
        Supplier<DomainException> onMismatch
    ) {
        Objects.requireNonNull(onMismatch, "onMismatch supplier cannot be null");

        if (expected == null) {
            return;
        }
        if (!Objects.equals(current, expected)) {
            throw onMismatch.get();
        }
    }
}
