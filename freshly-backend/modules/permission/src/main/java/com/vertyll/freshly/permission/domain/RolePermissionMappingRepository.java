package com.vertyll.freshly.permission.domain;

import com.vertyll.freshly.permission.Permission;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RolePermissionMappingRepository {

    RolePermissionMapping save(RolePermissionMapping mapping);

    List<RolePermissionMapping> findByKeycloakRoleIn(Set<String> roles);

    List<RolePermissionMapping> findByKeycloakRole(String role);

    void deleteById(UUID id);

    List<RolePermissionMapping> findAll();

    boolean existsByKeycloakRoleAndPermission(String keycloakRole, Permission permission);
}
