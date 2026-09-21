package com.vertyll.freshly.permission.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.infra.cache.CacheSpec;
import com.vertyll.freshly.permission.infrastructure.persistence.adapter.RoleAuthorityPersistenceAdapter;

@Configuration
public class PermissionCacheConfig {
    @Bean
    CacheSpec roleGrantsCache() {
        return new CacheSpec(RoleAuthorityPersistenceAdapter.GRANTS_CACHE);
    }
}
