package com.vertyll.freshly.security.resolver;

import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import lombok.RequiredArgsConstructor;

import com.vertyll.freshly.security.annotation.RefreshTokenCookie;
import com.vertyll.freshly.security.config.JwtProperties;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@SuppressFBWarnings(
        value = "COOKIE_USAGE",
        justification = "Refresh token is read server-side from a Secure, HttpOnly cookie")
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtProperties jwtProperties;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RefreshTokenCookie.class);
    }

    @Override
    public @Nullable Object resolveArgument(
            @NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        if (request == null || request.getCookies() == null) {
            return null;
        }

        String cookieName = jwtProperties.refreshToken().cookieName();

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
