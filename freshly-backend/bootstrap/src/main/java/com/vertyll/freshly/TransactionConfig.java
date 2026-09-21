package com.vertyll.freshly;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * The transaction manager every use-case proxy runs on.
 *
 * <p>
 * Declared here and not in {@code shared-infra} because which one to use is an assembly
 * decision: {@code TransactionalUseCaseFactory} asks for a {@code PlatformTransactionManager}
 * and has no opinion about what is behind it. Putting a Mongo-specific bean in the platform
 * would make the platform know the database, which is the coupling that module is written to
 * avoid.
 *
 * <p>
 * Spring Boot does not auto-configure one for MongoDB. Without this bean nothing starts:
 * every inbound port in the application is wrapped by that factory, and the factory cannot be
 * constructed.
 *
 * <p>
 * <strong>MongoDB must run as a replica set</strong>, single-node included. Multi-document
 * transactions are not available on a standalone server, so a standalone one fails at the
 * first commit rather than at start-up — the more confusing of the two failures. See the
 * README for the one-line local setup.
 */
@Configuration
public class TransactionConfig {

    @Bean
    MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
