package com.vertyll.freshly.infra.cache;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * The application's single cache manager, assembled from what the modules asked for.
 *
 * <p>
 * Spring collects every {@link CacheSpec} bean, so a module registers a cache by
 * declaring one and nothing here changes.
 *
 * <h2>This is in-memory, and that is a correctness limit rather than a performance one</h2>
 *
 * <p>
 * {@code ConcurrentMapCache} evicts locally. With a second instance running,
 * {@code permission}'s cache means revoking a grant on instance A leaves instance B
 * honoring it until restart — an authorization decision that stays wrong.
 * {@code airquality}'s station cache has no such problem, because a stale station list is
 * merely stale.
 *
 * <p>
 * Replacing this with Redis or Hazelcast is a change to this one file. Worth doing
 * before the second instance exists rather than after.
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Bean
    CacheManager cacheManager(List<CacheSpec> specs) {
        Set<String> names = specs.stream().map(CacheSpec::name).collect(Collectors.toUnmodifiableSet());

        if (names.size() != specs.size()) {
            // Two modules asking for the same cache name would share entries without
            // either knowing, which is why the naming convention prefixes the context.
            log.warn("Duplicate cache names among {} declarations: {}", specs.size(), names);
        }

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(names.stream().map(ConcurrentMapCache::new).toList());

        log.info("Registered caches: {}", names);
        return manager;
    }
}
