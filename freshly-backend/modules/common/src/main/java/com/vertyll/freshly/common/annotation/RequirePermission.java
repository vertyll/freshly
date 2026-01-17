package com.vertyll.freshly.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vertyll.freshly.common.enums.Permission;

/**
 * Annotation to secure endpoints with permission-based authorization.
 * Usage: @RequirePermission(Permission.USERS_READ) public ResponseEntity<?> getUser(...) { }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /** The permission required to access the method. */
    Permission value();
}
