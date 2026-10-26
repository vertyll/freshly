package com.vertyll.freshly;

import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;

@TestConfiguration(proxyBeanMethods = false)
class MongoTestContainer {

    @Bean
    @SuppressWarnings("resource")
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer("mongo:9").withStartupTimeout(Duration.ofMinutes(2))
            .withStartupAttempts(3)
            .withReplicaSet();
    }
}
