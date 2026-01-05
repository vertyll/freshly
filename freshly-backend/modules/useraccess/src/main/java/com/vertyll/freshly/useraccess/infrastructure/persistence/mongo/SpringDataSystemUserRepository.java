package com.vertyll.freshly.useraccess.infrastructure.persistence.mongo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataSystemUserRepository extends MongoRepository<SystemUserDocument, UUID> {
    Optional<SystemUserDocument> findByKeycloakUserId(UUID keycloakUserId);
}
