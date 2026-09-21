package com.vertyll.freshly.web.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the permission an endpoint requires.
 *
 * <p>
 * Losing compile-time checking of the value is real, and it is bought back at
 * start-up instead: {@code permission} refuses a grant naming a permission no
 * catalogue declared, so a typo surfaces as a failed boot rather than as an
 * endpoint nobody can reach. Controllers reference their own enum's
 * {@code value()}, so the string is never written by hand at a call site.
 */
@Target(
    {
        ElementType.METHOD,
        ElementType.TYPE
    }
)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** The permission's wire form, e.g. {@code users:read}. */
    String value();
}
