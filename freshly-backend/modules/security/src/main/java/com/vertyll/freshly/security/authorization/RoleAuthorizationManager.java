package com.vertyll.freshly.security.authorization;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.common.annotation.RequireRole;
import com.vertyll.freshly.common.enums.UserRoleEnum;

/** Authorization manager for @RequireRole annotation. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();

        // Check method-level annotation first
        RequireRole methodAnnotation = method.getAnnotation(RequireRole.class);
        if (methodAnnotation != null) {
            UserRoleEnum role = methodAnnotation.value();
            boolean granted = hasRole(authentication.get(), role);
            log.debug("Role check for method {}: {} = {}", method.getName(), role, granted);
            return new AuthorizationDecision(granted);
        }

        // Check class-level annotation
        RequireRole classAnnotation = method.getDeclaringClass().getAnnotation(RequireRole.class);
        if (classAnnotation != null) {
            UserRoleEnum role = classAnnotation.value();
            boolean granted = hasRole(authentication.get(), role);
            log.debug(
                    "Role check for class {}: {} = {}",
                    method.getDeclaringClass().getSimpleName(),
                    role,
                    granted);
            return new AuthorizationDecision(granted);
        }

        // No annotation found - deny by default
        log.warn("No @RequireRole annotation found on {} - denying access", method.getName());
        return new AuthorizationDecision(false);
    }

    private boolean hasRole(Authentication authentication, UserRoleEnum role) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(
                                authority -> role.getAuthority().equals(authority.getAuthority()));
    }
}
