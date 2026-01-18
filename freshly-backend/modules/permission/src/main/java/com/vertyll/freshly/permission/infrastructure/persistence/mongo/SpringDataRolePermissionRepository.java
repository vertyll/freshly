package com.vertyll.freshly.permission.infrastructure.persistence.mongo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.vertyll.freshly.common.enums.Permission;

public interface SpringDataRolePermissionRepository
        extends MongoRepository<RolePermissionMappingDocument, UUID> {

    List<RolePermissionMappingDocument> findByKeycloakRoleIn(Set<String> roles);

    List<RolePermissionMappingDocument> findByKeycloakRole(String role);

    boolean existsByKeycloakRoleAndPermission(String keycloakRole, Permission permission);
}
