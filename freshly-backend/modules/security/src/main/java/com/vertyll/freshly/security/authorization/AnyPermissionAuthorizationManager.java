package com.vertyll.freshly.security.authorization;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.common.annotation.RequireAnyPermission;
import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.permission.application.PermissionService;

@Slf4j
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@RequiredArgsConstructor
public class AnyPermissionAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private final PermissionService permissionService;

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();

        // Check method-level annotation first
        RequireAnyPermission methodAnnotation = method.getAnnotation(RequireAnyPermission.class);
        if (methodAnnotation != null) {
            Permission[] permissions = methodAnnotation.value(); // Bezpośrednio enum array!
            boolean granted = permissionService.hasAnyPermission(authentication.get(), permissions);
            log.debug(
                    "Any permission check for method {}: {} = {}",
                    method.getName(),
                    Arrays.toString(permissions),
                    granted);
            return new AuthorizationDecision(granted);
        }

        // Check class-level annotation
        RequireAnyPermission classAnnotation =
                method.getDeclaringClass().getAnnotation(RequireAnyPermission.class);
        if (classAnnotation != null) {
            Permission[] permissions = classAnnotation.value();
            boolean granted = permissionService.hasAnyPermission(authentication.get(), permissions);
            log.debug(
                    "Any permission check for class {}: {} = {}",
                    method.getDeclaringClass().getSimpleName(),
                    Arrays.toString(permissions),
                    granted);
            return new AuthorizationDecision(granted);
        }

        // No annotation found - deny by default
        log.warn(
                "No @RequireAnyPermission annotation found on {} - denying access",
                method.getName());
        return new AuthorizationDecision(false);
    }
}
