package com.vertyll.freshly;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.WriteConcern;

/**
 * What the MongoDB client waits for.
 *
 * <p>
 * A write is acknowledged once a majority of the replica set holds it, so a primary that
 * fails over does not take an acknowledged write with it. Connecting, choosing a server and
 * reading a reply each have a bound, so an unreachable database fails a request instead of
 * holding its thread.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MongoClientProperties.class)
class MongoClientConfig {

    @Bean
    MongoClientSettingsBuilderCustomizer mongoClientDurabilityAndTimeouts(MongoClientProperties properties) {
        return settings -> settings.writeConcern(WriteConcern.MAJORITY)
            .applyToClusterSettings(
                cluster -> cluster
                    .serverSelectionTimeout(properties.serverSelectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
            )
            .applyToSocketSettings(
                socket -> socket.connectTimeout(properties.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .readTimeout(properties.readTimeout().toMillis(), TimeUnit.MILLISECONDS)
            );
    }
}
