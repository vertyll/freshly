package com.vertyll.freshly.lang.error;

/**
 * What kind of failure an error describes, in terms every context shares.
 *
 * <p>
 * This is transport vocabulary, not domain knowledge. It exists so that one
 * place can decide which HTTP status a failure becomes, instead of every module
 * repeating the same mapping — and so that the layer which raises the failure
 * never has to name a status code it has no business knowing about.
 *
 * <p>
 * What a failure <em>means</em> stays in the module's own error catalogue,
 * which owns the key.
 *
 * <p>
 * These names look like HTTP statuses and are deliberately not. {@code NOT_FOUND} is
 * a statement about the request — the thing you named is not there — which is true
 * whether the caller arrived over HTTP, a message queue or a scheduled job. The mapping
 * to a status code lives in {@code shared-web}'s {@code ErrorHttpStatusMapper}, and it
 * is the only place that knows what a 404 is.
 *
 * <p>
 * This enum is the one genuine coupling point in the platform: adding a value
 * recompiles every module. Acceptable at five contexts, worth revisiting at fifteen —
 * at which point the answer is a per-module mapper rather than a longer enum.
 */
public enum ErrorKind {
    NOT_FOUND,
    UNAUTHENTICATED,
    ACCESS_DENIED,
    CONFLICT,
    INVALID,
    PRECONDITION_FAILED,
    GONE,
    MISCONFIGURED,
    EXTERNAL_SERVICE_FAILURE
}
