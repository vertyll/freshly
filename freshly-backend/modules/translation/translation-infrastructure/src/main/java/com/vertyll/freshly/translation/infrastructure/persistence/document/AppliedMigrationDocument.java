package com.vertyll.freshly.translation.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "translation_migration")
public record AppliedMigrationDocument(
    @Id String migrationId,
    @Field("context") String context,
    @Field("applied_at") Instant appliedAt
) {
}
