package com.vertyll.freshly.translation.infrastructure.resolver;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.translation.application.dto.TranslationBundle;
import com.vertyll.freshly.translation.application.port.inbound.query.TranslationQueryUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CachedTranslationBundles {
    public static final String BUNDLES_CACHE = "translation-bundles";

    private final TranslationQueryUseCase queries;

    @Cacheable(value = BUNDLES_CACHE, key = "#language")
    public TranslationBundle bundleFor(String language) {
        return queries.bundleFor(language);
    }

    @CacheEvict(value = BUNDLES_CACHE, allEntries = true)
    public void invalidate() {
    }
}
