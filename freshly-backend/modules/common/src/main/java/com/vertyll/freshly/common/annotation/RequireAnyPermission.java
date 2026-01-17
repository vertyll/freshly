package com.vertyll.freshly.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vertyll.freshly.common.enums.Permission;

/**
 * Annotation to secure endpoints requiring ANY of the specified permissions.
 * Usage: @RequireAnyPermission({Permission.USERS_READ, Permission.USERS_WRITE}) public
 * ResponseEntity<?> manageUser(...) { }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAnyPermission {
    /** Array of permissions - user needs at least one. */
    Permission[] value();
}
