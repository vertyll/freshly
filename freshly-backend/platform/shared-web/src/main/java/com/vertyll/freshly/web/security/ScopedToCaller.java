package com.vertyll.freshly.web.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that an endpoint needs no permission because it only ever returns or
 * changes the caller's own data.
 *
 * <p>
 * Carries no behaviour. It exists so that "this endpoint is guarded elsewhere"
 * and "nobody remembered to guard this endpoint" stop looking identical in the
 * source — a distinction that otherwise only surfaces when a caller reaches
 * something they should not.
 */
@Target(
    {
        ElementType.METHOD,
        ElementType.TYPE
    }
)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScopedToCaller {

    /** Why no permission check applies, in a sentence. */
    String value();
}
