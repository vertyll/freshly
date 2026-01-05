package com.vertyll.freshly.security.authorization;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.permission.application.PermissionService;
import com.vertyll.freshly.security.annotation.RequirePermission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Authorization manager for @RequirePermission annotation. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private final PermissionService permissionService;

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();

        // Check method-level annotation first
        RequirePermission methodAnnotation = method.getAnnotation(RequirePermission.class);
        if (methodAnnotation != null) {
            String permission = methodAnnotation.value();
            boolean granted = permissionService.hasPermission(authentication.get(), permission);
            log.debug(
                    "Permission check for method {}: {} = {}",
                    method.getName(),
                    permission,
                    granted);
            return new AuthorizationDecision(granted);
        }

        // Check class-level annotation
        RequirePermission classAnnotation =
                method.getDeclaringClass().getAnnotation(RequirePermission.class);
        if (classAnnotation != null) {
            String permission = classAnnotation.value();
            boolean granted = permissionService.hasPermission(authentication.get(), permission);
            log.debug(
                    "Permission check for class {}: {} = {}",
                    method.getDeclaringClass().getSimpleName(),
                    permission,
                    granted);
            return new AuthorizationDecision(granted);
        }

        // No annotation found - deny by default
        log.warn("No @RequirePermission annotation found on {} - denying access", method.getName());
        return new AuthorizationDecision(false);
    }
}
