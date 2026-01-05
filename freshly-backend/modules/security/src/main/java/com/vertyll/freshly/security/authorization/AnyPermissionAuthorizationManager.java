package com.vertyll.freshly.security.authorization;

import com.vertyll.freshly.security.annotation.RequireAnyPermission;
import com.vertyll.freshly.permission.application.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Authorization manager for @RequireAnyPermission annotation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnyPermissionAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private final PermissionService permissionService;

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();

        // Check method-level annotation first
        RequireAnyPermission methodAnnotation = method.getAnnotation(RequireAnyPermission.class);
        if (methodAnnotation != null) {
            String[] permissions = methodAnnotation.value();
            boolean granted = permissionService.hasAnyPermission(authentication.get(), permissions);
            log.debug(
                    "Any permission check for method {}: {} = {}",
                    method.getName(),
                    Arrays.toString(permissions),
                    granted
            );
            return new AuthorizationDecision(granted);
        }

        // Check class-level annotation
        RequireAnyPermission classAnnotation = method.getDeclaringClass().getAnnotation(RequireAnyPermission.class);
        if (classAnnotation != null) {
            String[] permissions = classAnnotation.value();
            boolean granted = permissionService.hasAnyPermission(authentication.get(), permissions);
            log.debug(
                    "Any permission check for class {}: {} = {}",
                    method.getDeclaringClass().getSimpleName(),
                    Arrays.toString(permissions),
                    granted
            );
            return new AuthorizationDecision(granted);
        }

        // No annotation found - deny by default
        log.warn("No @RequireAnyPermission annotation found on {} - denying access", method.getName());
        return new AuthorizationDecision(false);
    }
}