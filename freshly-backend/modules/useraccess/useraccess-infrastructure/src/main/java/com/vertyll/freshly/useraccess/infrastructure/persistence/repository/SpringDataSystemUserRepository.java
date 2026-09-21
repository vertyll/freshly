package com.vertyll.freshly.useraccess.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.vertyll.freshly.useraccess.infrastructure.persistence.document.SystemUserDocument;

public interface SpringDataSystemUserRepository extends MongoRepository<SystemUserDocument, UUID> {
}
