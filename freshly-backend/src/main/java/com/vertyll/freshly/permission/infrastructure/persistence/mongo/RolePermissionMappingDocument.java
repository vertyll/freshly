package com.vertyll.freshly.permission.infrastructure.persistence.mongo;

import com.vertyll.freshly.permission.Permission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "role_permission_mapping")
@CompoundIndex(
        name = "role_permission_idx",
        def = "{'keycloakRole': 1, 'permission': 1}",
        unique = true
)
@Data
@NoArgsConstructor
@AllArgsConstructor
class RolePermissionMappingDocument {

    @Id
    private UUID id;

    private String keycloakRole;

    private Permission permission;
}
