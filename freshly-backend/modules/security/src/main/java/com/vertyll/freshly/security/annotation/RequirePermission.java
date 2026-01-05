package com.vertyll.freshly.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to secure endpoints with permission-based authorization. Works with
 * PermissionAuthorizationManager in Spring Security 7+.
 *
 * <p>Usage:
 *
 * <pre>
 * {@code @RequirePermission("users:create")}
 * public ResponseEntity<?> createUser(...) { }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * The permission value required to access the method. Should match a Permission enum value
     * (e.g., "users:create").
     */
    String value();
}
