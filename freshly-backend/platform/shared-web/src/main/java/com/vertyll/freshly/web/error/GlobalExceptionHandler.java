package com.vertyll.freshly.web.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.error.ErrorKind;
import com.vertyll.freshly.web.i18n.MessageResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Answers every refusal as an RFC 9457 problem document, whichever module threw it.
 *
 * <p>
 * Spring's own failures — an unsupported method, a missing parameter, an unknown path
 * — answer in the same shape through {@code spring.mvc.problemdetails.enabled}. The ones
 * handled here are handled because they carry something extra worth telling the caller:
 * which fields were rejected.
 *
 * <p>
 * Ordered ahead of Boot's advice so these win, and short of the highest precedence so
 * a module could still put its own in front.
 */
@RestControllerAdvice
@Order(GlobalExceptionHandler.ADVICE_ORDER)
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    static final int ADVICE_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    private static final String ACCESS_DENIED = "error.security.accessDenied";
    private static final String UNAUTHENTICATED = "error.security.unauthenticated";
    private static final String VALIDATION_FAILED = "error.common.validationFailed";
    private static final String UNEXPECTED_ERROR = "error.common.unexpectedError";
    private static final String UNREADABLE_BODY = "validation.unreadableBody";
    private static final String VALIDATION_INVALID = "validation.invalid";
    private static final String TYPE_MISMATCH = "validation.typeMismatch";
    private static final String CONCURRENT_MODIFICATION = "error.common.concurrentModification";
    private static final String ALREADY_EXISTS = "error.common.alreadyExists";
    private static final String UPLOAD_TOO_LARGE = "error.common.uploadTooLarge";

    private static final String FIELDS = "fields";
    private static final String PARAMS = "params";
    private static final String URI_PREFIX = "uri=";

    private final MessageResolver messages;

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException exception, WebRequest request) {
        HttpStatus status = ErrorHttpStatusMapper.statusOf(exception.error().kind());

        if (exception.error().kind() == ErrorKind.MISCONFIGURED) {
            log.error("Misconfiguration: {} params={}", exception.error().key(), exception.params(), exception);
        } else {
            log.debug("Rejected request: {} params={}", exception.error().key(), exception.params());
        }

        return problem(
            status,
            exception.error().key(),
            exception.params(),
            request,
            exception.params().isEmpty() ? Map.of() : Map.of(PARAMS, exception.params())
        );
    }

    /**
     * Deliberately vague, and the same for both.
     *
     * <p>
     * Telling a caller whether they failed because the resource does not exist,
     * because they are not signed in, or because they lack one specific permission is a
     * small information leak repeated on every endpoint.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        log.debug("Access denied", exception);
        return problem(HttpStatus.FORBIDDEN, ACCESS_DENIED, request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleUnauthenticated(AuthenticationException exception, WebRequest request) {
        log.debug("Unauthenticated request", exception);
        return problem(HttpStatus.UNAUTHORIZED, UNAUTHENTICATED, request, Map.of());
    }

    /**
     * Body validation.
     *
     * <p>
     * Each rejected field carries a key rather than Hibernate Validator's English default,
     * so the whole API is consistent: {@code code} for a client to branch on, {@code message}
     * for a person to read, both from the translation store.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception, WebRequest request) {
        List<FieldViolation> fields = new ArrayList<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            String code;
            Map<String, Object> params;
            try {
                code = annotationNameOf(error);
                params = attributesOf(error);
            } catch (IllegalArgumentException _) {
                code = VALIDATION_INVALID;
                params = Map.of();
            }

            fields.add(new FieldViolation(error.getField(), code, resolve(code, params), params));
        }

        log.debug("Validation rejected {} field(s)", fields.size());
        return validationProblem(fields, request);
    }

    /** Bean validation on a query parameter or path variable, rather than on a body. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception, WebRequest request) {
        List<FieldViolation> fields = new ArrayList<>();

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String code = ConstraintCodes.codeOf(violation);
            Map<String, Object> params = ConstraintCodes.paramsOf(violation);

            fields.add(new FieldViolation(lastNode(violation), code, resolve(code, params), params));
        }

        log.debug("Constraint violation on {} field(s)", fields.size());
        return validationProblem(fields, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception, WebRequest request) {
        log.debug("Unparseable request parameter '{}'", exception.getName());

        Map<String, Object> params = Map.of("parameter", exception.getName());
        return validationProblem(
            List.of(new FieldViolation(exception.getName(), TYPE_MISMATCH, resolve(TYPE_MISMATCH, params), params)),
            request
        );
    }

    /**
     * Malformed JSON, or a body that does not fit the target type.
     *
     * <p>
     * No field list: the parse failed, so there is nothing reliable to name, and
     * Jackson's own message can quote the offending input back — which is a way to
     * reflect un sanitised request content into a response.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception, WebRequest request) {
        log.debug("Unreadable request body", exception);

        return problem(HttpStatus.BAD_REQUEST, UNREADABLE_BODY, request, Map.of());
    }

    /**
     * Two writers reached the same document at once.
     *
     * <p>
     * 409 rather than the 412 an {@code If-Match} mismatch answers with, because the two
     * say different things. 412 means the precondition the caller stated did not hold; this
     * means the caller stated none and lost a race it did not know it was in. Retrying is
     * the right response to one and not to the other.
     *
     * <p>
     * Without this the store's own locking failure would reach the catch-all and answer
     * 500, which invites a client to retry something that was never the server's fault.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentModification(OptimisticLockingFailureException exception, WebRequest request) {
        log.debug("Concurrent modification", exception);
        return problem(HttpStatus.CONFLICT, CONCURRENT_MODIFICATION, request, Map.of());
    }

    /**
     * A unique index refused to write.
     *
     * <p>
     * Every path that can raise this checks for the clash first, so reaching here means
     * two requests passed that check at the same moment. The index is what makes the race
     * safe; this is what makes it legible.
     *
     * <p>
     * Deliberately says nothing about which value collided: the exception carries the
     * index name and the offending document, and both are ours, not the caller's business.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicate(DuplicateKeyException exception, WebRequest request) {
        log.debug("Unique index refused a write", exception);
        return problem(HttpStatus.CONFLICT, ALREADY_EXISTS, request, Map.of());
    }

    /** An upload over the configured ceiling, which is a 413 and not a server fault. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException exception, WebRequest request) {
        log.debug("Upload exceeded the configured maximum", exception);
        return problem(HttpStatus.CONTENT_TOO_LARGE, UPLOAD_TOO_LARGE, request, Map.of());
    }

    /**
     * The last resort, and deliberately not the first.
     *
     * <p>
     * Because this advice runs ahead of Boot's, Spring's own failures arrive here too.
     * Those already carry the status they deserve — an unknown path is a 404, not a 500 —
     * and their document is Spring's to build, so it is passed through untouched. Only
     * what nothing recognized becomes a 500.
     *
     * <p>
     * Such a document carries no {@code code}: that member names an entry in a
     * module's catalogue, and a request rejected before it reached the application has
     * none. A client reads {@code status} for these, which is what it means.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, WebRequest request) {
        if (exception instanceof ErrorResponse errorResponse) {
            log.debug(
                "Rejected by the framework: {} {}",
                errorResponse.getStatusCode(),
                exception.getClass().getSimpleName()
            );
            return errorResponse.getBody();
        }

        log.error("Unhandled exception", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR, request, Map.of());
    }

    private ProblemDetail validationProblem(List<FieldViolation> fields, WebRequest request) {
        return problem(
            HttpStatus.BAD_REQUEST,
            VALIDATION_FAILED,
            request,
            fields.isEmpty() ? Map.of() : Map.of(FIELDS, fields)
        );
    }

    /**
     * Resolves a constraint key, interpolating its own attributes by name.
     *
     * <p>
     * By name because the attribute map is what the constraint itself reports, so a
     * translation reads <code>"od {min} do {max} znaków"</code> and needs no agreement about
     * ordering with anybody. Positionally, the two numbers of a {@code @Size} are told apart
     * only by iteration order of a map, and getting them the wrong way round produces a
     * sentence that is confidently wrong rather than obviously broken.
     */
    private String resolve(String code, Map<String, Object> params) {
        return messages.resolve(code, params);
    }

    private ProblemDetail problem(HttpStatus status, String code, WebRequest request, Map<String, Object> properties) {
        return problem(status, code, Map.of(), request, properties);
    }

    /**
     * Builds the problem document, interpolating the error's own arguments into its text.
     *
     * <p>
     * The arguments are separate from the extension properties because they play two
     * different parts: they are what {@code detail} is rendered from, and they are also
     * published under {@code params} for a client that would rather write its own sentence.
     * Rendering without them is how a message reading "at most {limit} rows" reaches a
     * reader with the braces still in it.
     */
    private ProblemDetail problem(
        HttpStatus status,
        String code,
        Map<String, Object> arguments,
        WebRequest request,
        Map<String, Object> properties
    ) {
        return Problems.of(status, code, messages.resolve(code, arguments), instanceOf(request), properties);
    }

    private static String instanceOf(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith(URI_PREFIX) ? description.substring(URI_PREFIX.length()) : description;
    }

    /** The constraint annotation behind a Spring binding error. */
    private static String annotationNameOf(FieldError error) {
        return ConstraintCodes.codeOf(error.unwrap(ConstraintViolation.class));
    }

    /**
     * The constraint's attributes, read from the descriptor rather than guessed.
     *
     * <p>
     * Spring's {@code FieldError.getArguments()} is tempting and wrong: it is a
     * positional array whose meaning depends on the constraint — {@code [field, max, min]}
     * for {@code @Size}, {@code [field, regexp, flags]} for {@code @Email}, {@code [field,
     * value]} for {@code @Min}. Naming those positions {@code min} and {@code max} produces
     * a correct answer for exactly two annotations and nonsense for the rest, and a
     * translation interpolating them would print a regular expression where it promised a
     * number.
     *
     * <p>
     * The {@link ConstraintViolation} behind the error carries the attributes by name.
     * Spring exposes it on {@code FieldError} as the unwrapped source.
     */
    private static Map<String, Object> attributesOf(FieldError error) {
        ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);
        return ConstraintCodes.paramsOf(violation.getConstraintDescriptor().getAttributes());
    }

    private static String lastNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot < 0 ? path : path.substring(lastDot + 1);
    }
}
