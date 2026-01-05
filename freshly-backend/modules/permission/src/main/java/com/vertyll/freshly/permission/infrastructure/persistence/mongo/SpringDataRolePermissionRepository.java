package com.vertyll.freshly.permission.infrastructure.persistence.mongo;

import com.vertyll.freshly.permission.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SpringDataRolePermissionRepository extends MongoRepository<RolePermissionMappingDocument, UUID> {

    List<RolePermissionMappingDocument> findByKeycloakRoleIn(Set<String> roles);

    List<RolePermissionMappingDocument> findByKeycloakRole(String role);

    boolean existsByKeycloakRoleAndPermission(String keycloakRole, Permission permission);
}
