package com.vertyll.freshly.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ExternalServiceProperties.class})
public class ExternalServiceConfig {}
