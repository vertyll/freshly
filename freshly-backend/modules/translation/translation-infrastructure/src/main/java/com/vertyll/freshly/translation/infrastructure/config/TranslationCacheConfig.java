package com.vertyll.freshly.translation.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.infra.cache.CacheSpec;
import com.vertyll.freshly.translation.infrastructure.resolver.CachedTranslationBundles;
import com.vertyll.freshly.translation.infrastructure.resolver.StoredTranslationResolver;

@Configuration
public class TranslationCacheConfig {

    @Bean
    CacheSpec resolvedTranslationsCache() {
        return new CacheSpec(StoredTranslationResolver.TRANSLATIONS_CACHE);
    }

    @Bean
    CacheSpec translationBundlesCache() {
        return new CacheSpec(CachedTranslationBundles.BUNDLES_CACHE);
    }
}
