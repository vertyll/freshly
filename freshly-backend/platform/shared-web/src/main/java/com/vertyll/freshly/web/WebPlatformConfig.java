package com.vertyll.freshly.web;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The platform's web contribution, imported once by {@code bootstrap}.
 */
@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.web")
public class WebPlatformConfig {
}
