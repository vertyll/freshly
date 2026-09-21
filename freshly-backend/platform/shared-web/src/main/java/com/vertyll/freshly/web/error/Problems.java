package com.vertyll.freshly.web.error;

import java.net.URI;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Builds the RFC 9457 problem document this application answers refusals with.
 *
 * <p>
 * One place decides the shape, so a refusal from any module looks the same to a
 * client and the front end handles all of them one way. What changes is the key.
 *
 * <p>
 * The members, and why each is there:
 *
 * <ul>
 * <li>{@code type} — a URN identifying the problem. Stable, and the thing a client
 * should branch on.
 * <li>{@code code} — the bare catalogue key repeated as an extension member, so a
 * client can look it up without taking the URI apart. Redundant with {@code type}
 * on purpose: parsing a URI to recover a key is the kind of string surgery that
 * breaks when the prefix changes.
 * <li>{@code title} — the status reason phrase. Machine-stable, not translated.
 * <li>{@code detail} — the translated prose, resolved against the reader's locale.
 * <li>{@code instance} — the request URI, so a log line and a report can be matched.
 * </ul>
 *
 * <p>
 * {@code detail} is filled rather than left to the client. The translation store is one
 * in-process call away and the request carries an {@code Accept-Language}, so withholding the
 * prose would make every client redo work the server has already done — on the error path,
 * which is when things are already going wrong. {@code code} is still present, so a client
 * that wants its own wording is not forced to use ours.
 */
public final class Problems {

    private static final String TYPE_PREFIX = "urn:freshly:error:";
    private static final String CODE = "code";

    private Problems() {
    }

    public static ProblemDetail of(
        HttpStatus status,
        String code,
        @Nullable String detail,
        @Nullable String instance,
        Map<String, Object> properties
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(status);

        problem.setType(URI.create(TYPE_PREFIX + code));
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty(CODE, code);

        if (detail != null && !detail.isBlank()) {
            problem.setDetail(detail);
        }
        if (instance != null && !instance.isBlank()) {
            problem.setInstance(URI.create(instance));
        }
        properties.forEach(problem::setProperty);

        return problem;
    }
}
