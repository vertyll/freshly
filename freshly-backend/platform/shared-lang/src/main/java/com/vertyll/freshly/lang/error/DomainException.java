package com.vertyll.freshly.lang.error;

import java.io.Serial;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Refuses a request, naming the error from the raising module's own catalogue.
 *
 * <p>
 * Carries no HTTP status: the status follows from {@link ErrorKind}, and the
 * layer that throws this has no business knowing about HTTP.
 *
 * <p>
 * Final: the exception handler reads {@link #error()} to choose a status, and a
 * subclass overriding it would change how every module's refusals are answered.
 */
public final class DomainException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String ERROR_CANNOT_BE_NULL = "Domain error cannot be null";
    private static final String PARAMS_CANNOT_BE_NULL = "Params cannot be null";

    private final DomainError error;

    /** Values the translated message interpolates, e.g. the id that was not found. */
    @SuppressWarnings("java:S1948")
    private final Map<String, Object> params;

    public DomainException(DomainError error) {
        this(error, Map.of());
    }

    public DomainException(DomainError error, Map<String, Object> params) {
        super(requireNonNull(error, ERROR_CANNOT_BE_NULL).key());
        this.error = error;
        this.params = Map.copyOf(requireNonNull(params, PARAMS_CANNOT_BE_NULL));
    }

    /**
     * Carries the underlying failure, for a refusal an adapter translates from something
     * else — an SMTP rejection, an unreadable upload.
     *
     * <p>
     * The cause never reaches the caller: the response carries {@code error.key()}.
     * It exists so the log line has the reason, which is otherwise lost at the point of
     * translation.
     */
    public DomainException(DomainError error, Map<String, Object> params, Throwable cause) {
        super(requireNonNull(error, ERROR_CANNOT_BE_NULL).key(), cause);
        this.error = error;
        this.params = Map.copyOf(requireNonNull(params, PARAMS_CANNOT_BE_NULL));
    }

    public DomainError error() {
        return error;
    }

    public Map<String, Object> params() {
        return params;
    }
}
