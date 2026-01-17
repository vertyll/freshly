package com.vertyll.freshly.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vertyll.freshly.common.enums.UserRoleEnum;

/**
 * Annotation to secure endpoints requiring ALL the specified roles.
 * Usage: @RequireAllRoles({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_USER}) public ResponseEntity<?>
 * superAdminsOnly(...) { }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAllRoles {
    /** Array of roles - user must have all of them. */
    UserRoleEnum[] value();
}
