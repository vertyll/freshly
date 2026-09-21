package com.vertyll.freshly.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;

import com.vertyll.freshly.authz.CallerRoles;
import com.vertyll.freshly.authz.PermissionEvaluator;
import com.vertyll.freshly.web.security.RequireAnyPermission;
import com.vertyll.freshly.web.security.RequirePermission;

import lombok.RequiredArgsConstructor;

/**
 * Enforces {@code @RequirePermission} and {@code @RequireAnyPermission}.
 *
 * <p>
 * Reads the annotation from the invoked method, falling back to the class, so
 * {@code @RequirePermission} on a controller guards every handler on it.
 * {@code AnnotatedElementUtils} rather than {@code getAnnotation} because the latter does
 * not see an annotation inherited through a meta-annotation or a proxied method.
 */
@RequiredArgsConstructor
public class PermissionMethodAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private final PermissionEvaluator permissions;

    @Override
    public @Nullable AuthorizationDecision authorize(
        Supplier<? extends @Nullable Authentication> authentication,
        MethodInvocation invocation
    ) {
        Method method = invocation.getMethod();
        CallerRoles roles = AuthenticationRoles.of(authentication.get());

        RequirePermission required = find(method, invocation, RequirePermission.class);
        if (required != null) {
            return new AuthorizationDecision(permissions.permits(roles, required.value()));
        }

        RequireAnyPermission anyOf = find(method, invocation, RequireAnyPermission.class);
        if (anyOf != null) {
            // copyOf, not Set.of: the latter throws on a repeated value, so an annotation
            // naming one permission twice would fail every request to that handler.
            return new AuthorizationDecision(permissions.permitsAny(roles, Set.copyOf(Arrays.asList(anyOf.value()))));
        }

        // No annotation means this manager should not have been consulted. Abstaining
        // (null) rather than denying lets the filter chain's own rules decide, which is
        // what happens for `@PublicEndpoint` and `@ScopedToCaller` handlers.
        return null;
    }

    private static <A extends Annotation> @Nullable A find(Method method, MethodInvocation invocation, Class<A> type) {
        A onMethod = AnnotatedElementUtils.findMergedAnnotation(method, type);
        if (onMethod != null) {
            return onMethod;
        }
        Object target = invocation.getThis();
        return target == null ? null : AnnotatedElementUtils.findMergedAnnotation(target.getClass(), type);
    }
}
