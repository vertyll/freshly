package com.vertyll.freshly.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vertyll.freshly.common.enums.UserRoleEnum;

/**
 * Annotation to secure endpoints requiring ANY of the specified roles.
 * Usage: @RequireAnyRole({UserRoleEnum.ADMIN, UserRoleEnum.MODERATOR}) public ResponseEntity<?>
 * moderatorsOrAdmins(...) { }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAnyRole {
    /** Array of roles - user needs at least one. */
    UserRoleEnum[] value();
}
