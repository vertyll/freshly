package com.vertyll.freshly.permission.infrastructure.persistence.mongo;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.permission.domain.RolePermissionMapping;

@Component
class RolePermissionMappingMapper {

    RolePermissionMappingDocument toDocument(RolePermissionMapping mapping) {
        return new RolePermissionMappingDocument(
                mapping.getId(), mapping.getKeycloakRole(), mapping.getPermission(), null);
    }

    RolePermissionMapping toDomain(RolePermissionMappingDocument document) {
        return RolePermissionMapping.reconstitute(
                document.getId(),
                document.getKeycloakRole(),
                document.getPermission(),
                document.getVersion());
    }
}
