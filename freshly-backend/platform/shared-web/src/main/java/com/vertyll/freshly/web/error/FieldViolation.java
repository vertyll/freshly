package com.vertyll.freshly.web.error;

import java.util.Map;

/**
 * One rejected field, carrying a key rather than a sentence.
 *
 * <p>
 * Each violation names a key the same way a {@code DomainException} does, so the whole
 * API is consistent: a client reads {@code code}, a human reads {@code message}, and both
 * come from the same place.
 *
 * <p>
 * Answering with Hibernate Validator's own default — {@code "size must be between 3 and
 * 50"} — would give a string that is English regardless of {@code Accept-Language}, is not
 * in the translation store so nobody can edit it, and that a client cannot branch on
 * without matching prose.
 *
 * @param params the constraint's own arguments — {@code min}, {@code max}, {@code pattern}.
 *     Present so a client can render its own sentence, and so a translation can
 *     interpolate them rather than hard-coding the numbers into the text.
 */
public record FieldViolation(String field, String code, String message, Map<String, Object> params) {

    public FieldViolation {
        params = Map.copyOf(params);
    }
}
