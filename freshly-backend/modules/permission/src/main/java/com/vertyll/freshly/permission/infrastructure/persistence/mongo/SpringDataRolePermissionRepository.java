package com.vertyll.freshly.permission.infrastructure.persistence.mongo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.common.enums.UserRoleEnum;

public interface SpringDataRolePermissionRepository
        extends MongoRepository<RolePermissionMappingDocument, UUID> {

    List<RolePermissionMappingDocument> findByKeycloakRoleIn(Set<UserRoleEnum> roles);

    List<RolePermissionMappingDocument> findByKeycloakRole(UserRoleEnum role);

    boolean existsByKeycloakRoleAndPermission(UserRoleEnum keycloakRole, Permission permission);
}
