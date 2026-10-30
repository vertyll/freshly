package com.vertyll.freshly.web.error;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.error.ErrorKind;
import com.vertyll.freshly.web.i18n.MessageResolver;

import static java.util.Objects.requireNonNull;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private static final String KEY = "error.test.refused";

    private static final Map<String, String> TEXTS = Map.of(
        KEY,
        "Refused {id}",
        "validation.size",
        "From {min} to {max}",
        "validation.required",
        "Required",
        "validation.typeMismatch",
        "Bad {parameter}",
        "error.common.unexpectedError",
        "Something went wrong"
    );

    private static ValidatorFactory factory;
    private static Validator validator;

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(new MessageResolver((key, _) -> Optional.ofNullable(TEXTS.get(key))));

    private final WebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/things/1"));

    @BeforeAll
    static void startValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void stopValidator() {
        factory.close();
    }

    @BeforeEach
    void anEnglishReader() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void noReader() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void aRefusalIsAnsweredWithTheStatusOfItsKindAndItsArguments() {
        ProblemDetail problem =
                handler.handleDomainException(new DomainException(TestError.MISSING, Map.of("id", "x")), request);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getType()).isEqualTo(URI.create("urn:freshly:error:" + KEY));
        assertThat(problem.getDetail()).isEqualTo("Refused x");
        assertThat(problem.getInstance()).isEqualTo(URI.create("/things/1"));
        assertThat(problem.getProperties()).containsEntry("code", KEY).containsEntry("params", Map.of("id", "x"));
    }

    @Test
    void aRefusalWithoutArgumentsPublishesNone() {
        ProblemDetail problem = handler.handleDomainException(new DomainException(TestError.BROKEN), request);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getProperties()).doesNotContainKey("params");
    }

    @Test
    void beingRefusedAndNotBeingSignedInAreToldApart() {
        assertThat(handler.handleAccessDenied(new AccessDeniedException("no"), request).getStatus()).isEqualTo(403);
        assertThat(handler.handleUnauthenticated(new BadCredentialsException("no"), request).getStatus())
            .isEqualTo(401);
    }

    @Test
    void aRejectedBodyNamesEachFieldWithItsTranslatedConstraint() throws NoSuchMethodException {
        Form form = new Form("ab", null);
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(form, "form");
        new SpringValidatorAdapter(validator).validate(form, result);
        Method method = Endpoint.class.getDeclaredMethod("accept", Form.class);

        ProblemDetail problem = handler
            .handleValidation(new MethodArgumentNotValidException(new MethodParameter(method, 0), result), request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getProperties()).containsEntry("code", "error.common.validationFailed");
        assertThat(fieldsOf(problem)).containsExactlyInAnyOrder(
            new FieldViolation("email", "validation.required", "Required", Map.of()),
            new FieldViolation("name", "validation.size", "From 3 to 5", Map.of("min", 3, "max", 5))
        );
    }

    @Test
    void aRejectedParameterIsNamedByTheLastPartOfItsPath() {
        ConstraintViolationException exception =
                new ConstraintViolationException(validator.validate(new Wrapper(new Form("abc", null))));

        ProblemDetail problem = handler.handleConstraintViolation(exception, request);

        assertThat(fieldsOf(problem))
            .containsExactly(new FieldViolation("email", "validation.required", "Required", Map.of()));
    }

    @Test
    void noViolationsMeanNoFieldList() {
        ProblemDetail problem = handler.handleConstraintViolation(new ConstraintViolationException(Set.of()), request);

        assertThat(problem.getProperties()).doesNotContainKey("fields");
    }

    @Test
    void aParameterThatCannotBeParsedIsNamed() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
            "abc",
            Integer.class,
            "page",
            null,
            new NumberFormatException()
        );

        ProblemDetail problem = handler.handleTypeMismatch(exception, request);

        assertThat(fieldsOf(problem)).extracting(FieldViolation::message).containsExactly("Bad page");
    }

    @Test
    void anUnreadableBodyDoesNotQuoteTheInputBack() {
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("<script>", new MockHttpInputMessage(new byte[0]));

        ProblemDetail problem = handler.handleUnreadableBody(exception, request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).doesNotContain("<script>");
    }

    @Test
    void aLostRaceAndADuplicateAreConflicts() {
        assertThat(
            handler.handleConcurrentModification(new OptimisticLockingFailureException("x"), request).getStatus()
        ).isEqualTo(409);
        assertThat(handler.handleDuplicate(new DuplicateKeyException("index uk_email"), request).getDetail())
            .doesNotContain("uk_email");
    }

    @Test
    void anUploadOverTheLimitIsTooLarge() {
        ProblemDetail problem = handler.handleUploadTooLarge(new MaxUploadSizeExceededException(1L), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE.value());
    }

    @Test
    void aFailureSpringAlreadyAnsweredKeepsItsStatus() {
        ProblemDetail problem = handler.handleUnexpected(new ResponseStatusException(HttpStatus.NOT_FOUND), request);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getProperties()).isNull();
    }

    @Test
    void anythingUnknownIsAnInternalErrorThatTellsTheCallerNothing() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("the password is 123"), request);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("Something went wrong");
    }

    @SuppressWarnings("unchecked")
    private static List<FieldViolation> fieldsOf(ProblemDetail problem) {
        Map<String, @Nullable Object> properties = requireNonNull(problem.getProperties());
        return (List<FieldViolation>) requireNonNull(properties.get("fields"));
    }

    private interface Endpoint {
        void accept(Form form);
    }

    private record Form(@Size(min = 3, max = 5) String name, @NotNull @Nullable String email) {
    }

    private record Wrapper(@Valid Form form) {
    }

    private enum TestError implements DomainError {
        MISSING(ErrorKind.NOT_FOUND),
        BROKEN(ErrorKind.MISCONFIGURED);

        private final ErrorKind kind;

        TestError(ErrorKind kind) {
            this.kind = kind;
        }

        @Override
        public String key() {
            return KEY;
        }

        @Override
        public ErrorKind kind() {
            return kind;
        }
    }
}
