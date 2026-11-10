package com.vertyll.freshly;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.testcontainers.mongodb.MongoDBContainer;

import com.vertyll.freshly.airquality.infrastructure.persistence.document.AirQualityMeasurementDocument;
import com.vertyll.freshly.infra.mongo.MongoIndexes;
import com.vertyll.freshly.translation.infrastructure.persistence.document.TranslationKeyDocument;
import com.vertyll.freshly.useraccess.infrastructure.persistence.document.SystemUserDocument;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.IndexOptions;

import static org.assertj.core.api.Assertions.assertThat;

class MongoIndexesTest {
    private static final String DATABASE = "freshly";

    @SuppressWarnings("resource")
    private static final MongoDBContainer MONGO =
            new MongoDBContainer("mongo:9").withStartupTimeout(Duration.ofMinutes(2)).withStartupAttempts(3);

    private static MongoClient client;
    private static MongoTemplate template;

    @BeforeAll
    static void startMongo() {
        MONGO.start();
        client = MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(MONGO.getConnectionString()))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build()
        );
        SimpleMongoClientDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(client, DATABASE);
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setInitialEntitySet(
            Set.of(SystemUserDocument.class, TranslationKeyDocument.class, AirQualityMeasurementDocument.class)
        );
        mappingContext.setSimpleTypeHolder(new MongoCustomConversions(List.of()).getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), mappingContext);
        converter.afterPropertiesSet();
        template = new MongoTemplate(factory, converter);
    }

    @AfterAll
    static void stopMongo() {
        client.close();
        MONGO.stop();
    }

    @Test
    void indexesOfAnEarlierReleaseAreRenamedAndNewOnesCreated() {
        template.getCollection("useraccess_system_user").createIndex(new Document("is_active", 1), named("is_active"));
        template.getCollection("airquality_measurement")
            .createIndex(new Document("station_id", 1), named("station_id"));
        template.getCollection("airquality_measurement")
            .createIndex(
                new Document("station_id", 1).append("measured_at", -1),
                named("uk_station_measured_at").unique(true)
            );

        new MongoIndexes(template).afterSingletonsInstantiated();
        new MongoIndexes(template).afterSingletonsInstantiated();

        assertThat(indexNames("useraccess_system_user"))
            .containsExactlyInAnyOrder("_id_", "idx_useraccess_system_user_is_active");
        assertThat(indexNames("translation_key"))
            .containsExactlyInAnyOrder("_id_", "idx_translation_key_context", "idx_translation_key_declared");
        assertThat(indexNames("airquality_measurement")).containsExactlyInAnyOrder(
            "_id_",
            "idx_airquality_measurement_station_id",
            "uk_airquality_measurement_station_id_measured_at"
        );
        assertThat(
            index("airquality_measurement", "uk_airquality_measurement_station_id_measured_at").getBoolean("unique")
        ).isTrue();
    }

    private static IndexOptions named(String name) {
        return new IndexOptions().name(name);
    }

    private static List<String> indexNames(String collection) {
        return indexes(collection).stream().map(index -> index.getString("name")).toList();
    }

    private static Document index(String collection, String name) {
        return indexes(collection).stream()
            .filter(index -> name.equals(index.getString("name")))
            .findFirst()
            .orElseThrow();
    }

    private static List<Document> indexes(String collection) {
        List<Document> indexes = new ArrayList<>();
        template.getCollection(collection).listIndexes().forEach(indexes::add);
        return indexes;
    }
}
