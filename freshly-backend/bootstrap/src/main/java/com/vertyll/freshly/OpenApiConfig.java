package com.vertyll.freshly;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.authz.CallerRoles;

@Configuration
public class OpenApiConfig {
    private OpenApiConfig() {
    }

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CallerRoles.class);
    }
}
