package com.vertyll.freshly.common.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Configuration for internationalization (i18n) support. Supports en-US and pl locales for API
 * responses, emails, and validation messages.
 */
@Configuration
public class I18nConfig {

    /**
     * Configures MessageSource for loading localized messages from properties files. Message files:
     * i18n/messages_en.properties, i18n/messages_pl.properties
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        // Load both application messages and validation messages
        messageSource.setBasenames("i18n/messages", "ValidationMessages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(Locale.US);
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true); // Return code if message not found
        return messageSource;
    }

    /**
     * Configures LocaleResolver to determine user's locale from Accept-Language header. Falls back
     * to en-US if header is missing or unsupported.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setSupportedLocales(List.of(Locale.US, Locale.forLanguageTag("pl")));
        localeResolver.setDefaultLocale(Locale.US);
        return localeResolver;
    }
}
