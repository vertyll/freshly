package com.vertyll.freshly.airquality.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.airquality.infrastructure.persistence.adapter.StationCache;
import com.vertyll.freshly.infra.cache.CacheSpec;

@Configuration
public class AirQualityCacheConfig {
    @Bean
    CacheSpec giosStationsCache() {
        return new CacheSpec(StationCache.STATIONS_CACHE);
    }

}
