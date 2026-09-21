package com.vertyll.freshly.permission.infrastructure.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.vertyll.freshly.permission.infrastructure.persistence.document.RoleAuthorityDocument;

public interface SpringDataRoleAuthorityRepository extends MongoRepository<RoleAuthorityDocument, String> {
}
