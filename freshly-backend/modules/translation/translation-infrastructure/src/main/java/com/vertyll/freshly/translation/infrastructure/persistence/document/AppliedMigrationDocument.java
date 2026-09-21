package com.vertyll.freshly.translation.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "translation_migration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppliedMigrationDocument {
    @Id
    @Field("migration_id")
    private String migrationId;

    @Field("context")
    private String context;

    @Field("applied_at")
    private Instant appliedAt;
}
