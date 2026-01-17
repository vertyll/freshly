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

import com.vertyll.freshly.common.annotation.RequireAllRoles;
import com.vertyll.freshly.common.enums.UserRoleEnum;

/** Authorization manager for @RequireAllRoles annotation. */
@Slf4j
@Component
public class AllRolesAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();

        // Check method-level annotation first
        RequireAllRoles methodAnnotation = method.getAnnotation(RequireAllRoles.class);
        if (methodAnnotation != null) {
            UserRoleEnum[] roles = methodAnnotation.value();
            boolean granted = hasAllRoles(authentication.get(), roles);
            log.debug(
                    "All roles check for method {}: {} = {}",
                    method.getName(),
                    Arrays.toString(roles),
                    granted);
            return new AuthorizationDecision(granted);
        }

        // Check class-level annotation
        RequireAllRoles classAnnotation =
                method.getDeclaringClass().getAnnotation(RequireAllRoles.class);
        if (classAnnotation != null) {
            UserRoleEnum[] roles = classAnnotation.value();
            boolean granted = hasAllRoles(authentication.get(), roles);
            log.debug(
                    "All roles check for class {}: {} = {}",
                    method.getDeclaringClass().getSimpleName(),
                    Arrays.toString(roles),
                    granted);
            return new AuthorizationDecision(granted);
        }

        // No annotation found - deny by default
        log.warn("No @RequireAllRoles annotation found on {} - denying access", method.getName());
        return new AuthorizationDecision(false);
    }

    private boolean hasAllRoles(Authentication authentication, UserRoleEnum... roles) {
        if (authentication == null) {
            return false;
        }
        for (UserRoleEnum role : roles) {
            boolean hasRole =
                    authentication.getAuthorities().stream()
                            .anyMatch(
                                    authority ->
                                            role.getAuthority().equals(authority.getAuthority()));
            if (!hasRole) {
                return false;
            }
        }
        return true;
    }
}
