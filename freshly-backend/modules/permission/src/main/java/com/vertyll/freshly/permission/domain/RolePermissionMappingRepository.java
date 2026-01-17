package com.vertyll.freshly.permission.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.permission.Permission;

public interface RolePermissionMappingRepository {

    RolePermissionMapping save(RolePermissionMapping mapping);

    List<RolePermissionMapping> findByKeycloakRoleIn(Set<UserRoleEnum> roles);

    List<RolePermissionMapping> findByKeycloakRole(UserRoleEnum role);

    void deleteById(UUID id);

    List<RolePermissionMapping> findAll();

    boolean existsByKeycloakRoleAndPermission(UserRoleEnum keycloakRole, Permission permission);
}
