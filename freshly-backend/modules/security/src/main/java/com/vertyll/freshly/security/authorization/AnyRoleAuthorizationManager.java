package com.vertyll.freshly.security.authorization;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.common.annotation.RequireAnyRole;
import com.vertyll.freshly.common.enums.UserRoleEnum;

/** Authorization manager for @RequireAnyRole annotation. */
@Slf4j
@Component
public class AnyRoleAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();

        // Check method-level annotation first
        RequireAnyRole methodAnnotation = method.getAnnotation(RequireAnyRole.class);
        if (methodAnnotation != null) {
            UserRoleEnum[] roles = methodAnnotation.value();
            boolean granted = hasAnyRole(authentication.get(), roles);
            log.debug(
                    "Any role check for method {}: {} = {}",
                    method.getName(),
                    Arrays.toString(roles),
                    granted);
            return new AuthorizationDecision(granted);
        }

        // Check class-level annotation
        RequireAnyRole classAnnotation =
                method.getDeclaringClass().getAnnotation(RequireAnyRole.class);
        if (classAnnotation != null) {
            UserRoleEnum[] roles = classAnnotation.value();
            boolean granted = hasAnyRole(authentication.get(), roles);
            log.debug(
                    "Any role check for class {}: {} = {}",
                    method.getDeclaringClass().getSimpleName(),
                    Arrays.toString(roles),
                    granted);
            return new AuthorizationDecision(granted);
        }

        // No annotation found - deny by default
        log.warn("No @RequireAnyRole annotation found on {} - denying access", method.getName());
        return new AuthorizationDecision(false);
    }

    private boolean hasAnyRole(Authentication authentication, UserRoleEnum... roles) {
        if (authentication == null) {
            return false;
        }
        for (UserRoleEnum role : roles) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> role.getAuthority().equals(authority.getAuthority()))) {
                return true;
            }
        }
        return false;
    }
}
