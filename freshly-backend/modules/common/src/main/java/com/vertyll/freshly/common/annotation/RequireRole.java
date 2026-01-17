package com.vertyll.freshly.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vertyll.freshly.common.enums.UserRoleEnum;

/**
 * Annotation to secure endpoints with role-based authorization.
 * Usage: @RequireRole(UserRoleEnum.ADMIN) public ResponseEntity<?> adminOnly(...) { }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /** The role required to access the method. */
    UserRoleEnum value();
}
