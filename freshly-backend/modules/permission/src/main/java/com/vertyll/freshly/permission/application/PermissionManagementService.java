package com.vertyll.freshly.permission.application;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.api.dto.CreatePermissionMappingDto;
import com.vertyll.freshly.permission.api.dto.PermissionMappingResponseDto;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        Permission permission = Permission.fromValue(request.permission());

        if (repository.existsByKeycloakRoleAndPermission(request.keycloakRole(), permission)) {
            throw new IllegalArgumentException(
                    "Mapping already exists for role '"
                            + request.keycloakRole()
                            + "' and permission '"
                            + request.permission()
                            + "'");
        }

        RolePermissionMapping mapping =
                new RolePermissionMapping(request.keycloakRole(), permission);

        RolePermissionMapping saved = repository.save(mapping);

        log.info(
                "Created permission mapping: {} -> {}",
                request.keycloakRole(),
                request.permission());

        return toDto(saved);
    }

    @CacheEvict(value = "user-permissions", allEntries = true)
    public void deleteMapping(UUID mappingId) {
        repository.deleteById(mappingId);
        log.info("Deleted permission mapping: {}", mappingId);
    }

    private PermissionMappingResponseDto toDto(RolePermissionMapping mapping) {
        return new PermissionMappingResponseDto(
                mapping.getId(), mapping.getKeycloakRole(), mapping.getPermission().getValue());
    }
}
