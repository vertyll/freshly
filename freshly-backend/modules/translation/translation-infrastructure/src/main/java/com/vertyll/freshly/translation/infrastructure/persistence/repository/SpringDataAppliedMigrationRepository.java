package com.vertyll.freshly.translation.infrastructure.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.vertyll.freshly.translation.infrastructure.persistence.document.AppliedMigrationDocument;

public interface SpringDataAppliedMigrationRepository extends MongoRepository<AppliedMigrationDocument, String> {
}
