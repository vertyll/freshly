package com.vertyll.freshly.permission.infrastructure.persistence.mongo;

import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository("rolePermissionMappingRepository")
@RequiredArgsConstructor
class MongoRolePermissionMappingRepository implements RolePermissionMappingRepository {

    private final SpringDataRolePermissionRepository springDataRepository;
    private final RolePermissionMappingMapper mapper;

    @Override
    public RolePermissionMapping save(RolePermissionMapping mapping) {
        RolePermissionMappingDocument document = mapper.toDocument(mapping);
        RolePermissionMappingDocument saved = springDataRepository.save(document);
        return mapper.toDomain(saved);
    }

    @Override
    public List<RolePermissionMapping> findByKeycloakRoleIn(Set<String> roles) {
        return springDataRepository.findByKeycloakRoleIn(roles).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RolePermissionMapping> findByKeycloakRole(String role) {
        return springDataRepository.findByKeycloakRole(role).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public List<RolePermissionMapping> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByKeycloakRoleAndPermission(String keycloakRole, Permission permission) {
        return springDataRepository.existsByKeycloakRoleAndPermission(keycloakRole, permission);
    }
}
