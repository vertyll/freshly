package com.vertyll.freshly.security;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.vertyll.freshly.authz.CallerRoles;

/**
 * Lets a controller take the caller's roles as a {@link CallerRoles} parameter.
 *
 * <p>
 * A controller in a module then names only {@code shared-authz}, which it already has,
 * rather than {@code shared-security}, which only {@code bootstrap} depends on. The
 * conversion from a Spring Security {@code Authentication} stays in
 * {@link AuthenticationRoles}.
 */
public final class CallerRolesArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CallerRoles.class.equals(parameter.getParameterType());
    }

    @Override
    public CallerRoles resolveArgument(
        MethodParameter parameter,
        @Nullable ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        @Nullable WebDataBinderFactory binderFactory
    ) {
        return AuthenticationRoles.of(SecurityContextHolder.getContext().getAuthentication());
    }
}
