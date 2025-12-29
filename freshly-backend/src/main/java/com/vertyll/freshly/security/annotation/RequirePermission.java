package com.vertyll.freshly.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to secure endpoints with permission-based authorization.
 * <p>
 * Usage:
 * <pre>
 * {@code @RequirePermission("users:create")}
 * public ResponseEntity<?> createUser(...) { }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@permissionService.hasPermission(authentication, #permission)")
public @interface RequirePermission {

    /**
     * The permission value required to access the method.
     * Should match a Permission enum value (e.g., "users:create").
     */
    String value();
}
