package com.vertyll.freshly.web.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.metadata.ConstraintDescriptor;

/**
 * Turns a bean-validation constraint into a translation key and its arguments.
 *
 * <p>
 * Derived from the constraint <em>type</em> rather than read from a {@code message}
 * attribute on the annotation. That is the decision worth explaining.
 *
 * <p>
 * The obvious approach is {@code @Size(message = "{validation.username.tooLong}")}. It
 * works, and it keeps the text outside the store forever: Hibernate Validator resolves
 * {@code {...}} through its own {@code ResourceBundle} mechanism, not through Spring's
 * {@code MessageSource}, so those messages can never be edited at runtime. It also means a
 * key per field per constraint — hundreds of them, each written by hand.
 *
 * <p>
 * Deriving instead gives one key per constraint kind, interpolated with the constraint's
 * own attributes. {@code validation.size} with {@code min=3, max=50} covers every
 * {@code @Size} in the application, is editable like everything else, and needs no
 * annotation attribute anywhere.
 *
 * <p>
 * The cost is that a field cannot have bespoke wording. If one ever needs it, the fix is
 * a key derived from the field name with a fallback to the generic one — not a message
 * attribute.
 */
final class ConstraintCodes {

    private static final String PREFIX = "validation.";
    private static final String FALLBACK = PREFIX + "invalid";

    /** Constraint annotation simple name to key suffix. */
    private static final Map<String, String> BY_ANNOTATION = Map.ofEntries(
        Map.entry("NotNull", "required"),
        Map.entry("NotBlank", "required"),
        Map.entry("NotEmpty", "required"),
        Map.entry("Size", "size"),
        Map.entry("Min", "min"),
        Map.entry("Max", "max"),
        Map.entry("DecimalMin", "min"),
        Map.entry("DecimalMax", "max"),
        Map.entry("Positive", "positive"),
        Map.entry("PositiveOrZero", "positiveOrZero"),
        Map.entry("Negative", "negative"),
        Map.entry("Email", "email"),
        Map.entry("Pattern", "pattern"),
        Map.entry("Past", "past"),
        Map.entry("Future", "future"),
        Map.entry("AssertTrue", "mustBeTrue"),
        Map.entry("AssertFalse", "mustBeFalse")
    );

    /** Attributes worth passing through; the rest are validator plumbing. */
    private static final Set<String> INTERESTING = Set.of("min", "max", "value", "regexp", "inclusive");

    private ConstraintCodes() {
    }

    static String codeOf(ConstraintViolation<?> violation) {
        return codeOf(annotationNameOf(violation.getConstraintDescriptor()));
    }

    static String codeOf(String annotationSimpleName) {
        String suffix = BY_ANNOTATION.get(annotationSimpleName);
        // An unmapped constraint — a custom one, or one added to Jakarta later — falls back
        // rather than failing. A generic sentence beats a 500 raised while reporting a 400.
        return suffix == null ? FALLBACK : PREFIX + suffix;
    }

    static Map<String, Object> paramsOf(ConstraintViolation<?> violation) {
        return paramsOf(violation.getConstraintDescriptor().getAttributes());
    }

    static Map<String, Object> paramsOf(Map<String, Object> attributes) {
        // No null check on the value: an annotation member cannot hold null.
        Map<String, Object> params = new LinkedHashMap<>();
        attributes.forEach((name, value) -> {
            if (INTERESTING.contains(name)) {
                params.put(name, value);
            }
        });
        return params;
    }

    // Every descriptor carries its annotation; Bean Validation builds one from the other.
    private static String annotationNameOf(ConstraintDescriptor<?> descriptor) {
        return descriptor.getAnnotation().annotationType().getSimpleName();
    }
}
