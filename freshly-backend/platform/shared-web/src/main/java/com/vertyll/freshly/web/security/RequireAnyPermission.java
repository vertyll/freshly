package com.vertyll.freshly.web.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Permits the call when the caller holds at least one of the named permissions. */
@Target(
    {
        ElementType.METHOD,
        ElementType.TYPE
    }
)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAnyPermission {

    String[] value();
}
