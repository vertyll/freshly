package com.vertyll.freshly.security.resolver;

import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import lombok.RequiredArgsConstructor;

import com.vertyll.freshly.common.annotation.RefreshTokenCookie;
import com.vertyll.freshly.common.config.KeycloakProperties;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@SuppressFBWarnings(
        value = "COOKIE_USAGE",
        justification = "Refresh token is read server-side from a Secure, HttpOnly cookie")
@RequiredArgsConstructor
public class RefreshTokenCookieArgumentResolver implements HandlerMethodArgumentResolver {

    private final KeycloakProperties keycloakProperties;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RefreshTokenCookie.class)
                && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String cookieName = keycloakProperties.cookie().refreshTokenCookieName();

        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(this::extractCookieValue)
                .findFirst()
                .orElse(null);
    }

    private String extractCookieValue(Cookie cookie) {
        return cookie.getValue();
    }
}
