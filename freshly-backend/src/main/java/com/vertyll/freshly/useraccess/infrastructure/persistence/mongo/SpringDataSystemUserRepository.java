package com.vertyll.freshly.useraccess.infrastructure.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataSystemUserRepository extends MongoRepository<SystemUserDocument, UUID> {
    Optional<SystemUserDocument> findByKeycloakUserId(UUID keycloakUserId);
}
