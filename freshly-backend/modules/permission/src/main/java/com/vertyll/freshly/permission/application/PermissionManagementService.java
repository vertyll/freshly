package com.vertyll.freshly.permission.application;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.permission.api.dto.CreatePermissionMappingDto;
import com.vertyll.freshly.permission.api.dto.PermissionMappingResponseDto;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionManagementService {

    private final RolePermissionMappingRepository repository;

    public List<PermissionMappingResponseDto> getAllMappings() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public List<PermissionMappingResponseDto> getMappingsByRole(String role) {
        return repository.findByKeycloakRole(role).stream().map(this::toDto).toList();
    }

    @CacheEvict(value = "user-permissions", allEntries = true)
    public PermissionMappingResponseDto createMapping(CreatePermissionMappingDto request) {
        Permission permission = request.permission();
        String role = request.keycloakRole();

        if (repository.existsByKeycloakRoleAndPermission(role, permission)) {
            throw new IllegalArgumentException(
                    "Mapping already exists for role '"
                            + role
                            + "' and permission '"
                            + permission.getValue()
                            + "'");
        }

        RolePermissionMapping mapping = new RolePermissionMapping(role, permission);

        RolePermissionMapping saved = repository.save(mapping);

        log.info("Created permission mapping: {} -> {}", role, permission.getValue());

        return toDto(saved);
    }

    @CacheEvict(value = "user-permissions", allEntries = true)
    public void deleteMapping(UUID mappingId) {
        repository.deleteById(mappingId);
        log.info("Deleted permission mapping: {}", mappingId);
    }

    private PermissionMappingResponseDto toDto(RolePermissionMapping mapping) {
        return new PermissionMappingResponseDto(
                mapping.getId(), mapping.getKeycloakRole(), mapping.getPermission());
    }
}
