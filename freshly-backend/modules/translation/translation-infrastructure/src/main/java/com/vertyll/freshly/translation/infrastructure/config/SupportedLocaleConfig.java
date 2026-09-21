package com.vertyll.freshly.translation.infrastructure.config;

import java.util.Arrays;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import com.vertyll.freshly.translation.domain.model.SupportedLanguage;

@Configuration
public class SupportedLocaleConfig {

    @Bean(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(
            Arrays.stream(SupportedLanguage.values()).map(language -> Locale.of(language.tag())).toList()
        );
        resolver.setDefaultLocale(Locale.of(SupportedLanguage.SOURCE.tag()));
        return resolver;
    }
}
