package com.vertyll.freshly.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to secure endpoints requiring any of the specified permissions.
 * User needs at least one of the listed permissions to access the method.
 * <p>
 * Usage:
 * <pre>
 * {@code @RequireAnyPermission({"users:read", "users:create"})}
 * public ResponseEntity<?> getUsers(...) { }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@permissionService.hasPermission(authentication, #root.annotation.value())")
public @interface RequireAnyPermission {

    /**
     * Array of permission values. User needs at least one of these.
     */
    String[] value();
}
