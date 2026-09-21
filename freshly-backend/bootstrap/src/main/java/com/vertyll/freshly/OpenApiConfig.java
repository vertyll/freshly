package com.vertyll.freshly;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.authz.CallerRoles;

/**
 * Keeps values resolved from the security context out of the generated API description.
 *
 * <p>
 * A {@link CallerRoles} handler parameter is filled from the caller's token, not from
 * the request, so it must not appear as a query parameter a client is asked to send.
 */
@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CallerRoles.class);
    }
}
