package com.vertyll.freshly.web.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares that an endpoint is deliberately reachable without authentication. */
@Target(
    {
        ElementType.METHOD,
        ElementType.TYPE
    }
)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicEndpoint {

    String value();
}
