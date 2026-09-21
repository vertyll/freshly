package com.vertyll.freshly.security;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.web.error.Problems;
import com.vertyll.freshly.web.i18n.MessageResolver;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Answers filter-level refusals in the same problem shape as everything else.
 *
 * <p>
 * Necessary because a request rejected by the security filter chain never reaches a
 * controller, so {@code @RestControllerAdvice} never sees it. Without this, an expired
 * token produces Spring Security's default empty 401 with a {@code WWW-Authenticate}
 * header and no body, while an expired token caught inside a controller produces a
 * problem document — the same failure in two shapes depending on where it was noticed.
 *
 * <p>
 * Serialises with the application's {@code JsonMapper} rather than the message
 * converters, because at this point in the chain there is no {@code HandlerAdapter} to run
 * them.
 */
@Component
@RequiredArgsConstructor
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String UNAUTHENTICATED = "error.security.unauthenticated";
    private static final String ACCESS_DENIED = "error.security.accessDenied";

    private final MessageResolver messages;
    private final JsonMapper jsonMapper;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, UNAUTHENTICATED);
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, ACCESS_DENIED);
    }

    private void write(
        HttpServletRequest request,
        HttpServletResponse response,
        HttpStatus status,
        String code
    ) throws IOException {
        ProblemDetail problem = Problems.of(status, code, messages.resolve(code), request.getRequestURI(), Map.of());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
