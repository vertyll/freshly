package com.vertyll.freshly.translation.infrastructure.resolver;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationResolver;
import com.vertyll.freshly.translation.application.port.inbound.query.TranslationQueryUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StoredTranslationResolver implements TranslationResolver {
    public static final String TRANSLATIONS_CACHE = "translation-resolved";

    private final TranslationQueryUseCase queries;

    @Override
    @Cacheable(value = TRANSLATIONS_CACHE, key = "#key + '|' + #languageTag")
    public Optional<String> resolve(String key, String languageTag) {
        return queries.resolve(key, languageTag);
    }

    @CacheEvict(value = TRANSLATIONS_CACHE, allEntries = true)
    public void invalidate() {
    }
}
