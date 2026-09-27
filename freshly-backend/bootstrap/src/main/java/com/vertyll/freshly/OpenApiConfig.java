package com.vertyll.freshly;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.authz.CallerRoles;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {
    OpenApiConfig() {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CallerRoles.class);
    }
}
