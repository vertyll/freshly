package com.vertyll.freshly.permission.infrastructure.persistence.mongo;

import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import org.springframework.stereotype.Component;

@Component
class RolePermissionMappingMapper {

    public RolePermissionMappingDocument toDocument(RolePermissionMapping mapping) {
        return new RolePermissionMappingDocument(
                mapping.getId(),
                mapping.getKeycloakRole(),
                mapping.getPermission()
        );
    }

    public RolePermissionMapping toDomain(RolePermissionMappingDocument document) {
        return RolePermissionMapping.reconstitute(
                document.getId(),
                document.getKeycloakRole(),
                document.getPermission()
        );
    }
}