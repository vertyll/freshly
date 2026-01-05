package com.vertyll.freshly.permission.application;

import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.api.dto.CreatePermissionMappingDto;
import com.vertyll.freshly.permission.api.dto.PermissionMappingResponseDto;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionManagementServiceTest {

    @Mock
    private RolePermissionMappingRepository repository;

    @Captor
    private ArgumentCaptor<RolePermissionMapping> mappingCaptor;

    private PermissionManagementService service;

    @BeforeEach
    void setUp() {
        service = new PermissionManagementService(repository);
    }

    @Test
    @DisplayName("Should get all mappings")
    void shouldGetAllMappings() {
        // Given
        RolePermissionMapping mapping1 = new RolePermissionMapping(
                UUID.randomUUID(),
                "admin",
                Permission.USERS_READ
        );
        RolePermissionMapping mapping2 = new RolePermissionMapping(
                UUID.randomUUID(),
                "user",
                Permission.REPORTS_READ
        );
        when(repository.findAll()).thenReturn(List.of(mapping1, mapping2));

        // When
        List<PermissionMappingResponseDto> result = service.getAllMappings();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).keycloakRole()).isEqualTo("admin");
        assertThat(result.get(0).permission()).isEqualTo("users:read");
        assertThat(result.get(1).keycloakRole()).isEqualTo("user");
        assertThat(result.get(1).permission()).isEqualTo("reports:read");
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Should get mappings by role")
    void shouldGetMappingsByRole() {
        // Given
        String role = "admin";
        RolePermissionMapping mapping1 = new RolePermissionMapping(
                UUID.randomUUID(),
                role,
                Permission.USERS_READ
        );
        RolePermissionMapping mapping2 = new RolePermissionMapping(
                UUID.randomUUID(),
                role,
                Permission.USERS_CREATE
        );
        when(repository.findByKeycloakRole(role)).thenReturn(List.of(mapping1, mapping2));

        // When
        List<PermissionMappingResponseDto> result = service.getMappingsByRole(role);

        // Then
        assertThat(result)
                .hasSize(2)
                .allMatch(dto -> dto.keycloakRole().equals(role));
        verify(repository).findByKeycloakRole(role);
    }

    @Test
    @DisplayName("Should create new permission mapping")
    void shouldCreateNewPermissionMapping() {
        // Given
        CreatePermissionMappingDto request = new CreatePermissionMappingDto(
                "admin",
                "users:create"
        );

        when(repository.existsByKeycloakRoleAndPermission(
                request.keycloakRole(),
                Permission.USERS_CREATE
        )).thenReturn(false);

        RolePermissionMapping savedMapping = new RolePermissionMapping(
                UUID.randomUUID(),
                request.keycloakRole(),
                Permission.USERS_CREATE
        );
        when(repository.save(any(RolePermissionMapping.class))).thenReturn(savedMapping);

        // When
        PermissionMappingResponseDto result = service.createMapping(request);

        // Then
        assertThat(result.keycloakRole()).isEqualTo("admin");
        assertThat(result.permission()).isEqualTo("users:create");
        assertThat(result.id()).isEqualTo(savedMapping.getId());

        verify(repository).existsByKeycloakRoleAndPermission(
                request.keycloakRole(),
                Permission.USERS_CREATE
        );
        verify(repository).save(mappingCaptor.capture());

        RolePermissionMapping capturedMapping = mappingCaptor.getValue();
        assertThat(capturedMapping.getKeycloakRole()).isEqualTo("admin");
        assertThat(capturedMapping.getPermission()).isEqualTo(Permission.USERS_CREATE);
    }

    @Test
    @DisplayName("Should throw exception when mapping already exists")
    void shouldThrowExceptionWhenMappingAlreadyExists() {
        // Given
        CreatePermissionMappingDto request = new CreatePermissionMappingDto(
                "admin",
                "users:create"
        );

        when(repository.existsByKeycloakRoleAndPermission(
                request.keycloakRole(),
                Permission.USERS_CREATE
        )).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.createMapping(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mapping already exists")
                .hasMessageContaining("admin")
                .hasMessageContaining("users:create");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete mapping by id")
    void shouldDeleteMappingById() {
        // Given
        UUID mappingId = UUID.randomUUID();
        doNothing().when(repository).deleteById(mappingId);

        // When
        service.deleteMapping(mappingId);

        // Then
        verify(repository).deleteById(mappingId);
    }

    @Test
    @DisplayName("Should return empty list when no mappings exist")
    void shouldReturnEmptyListWhenNoMappingsExist() {
        // Given
        when(repository.findAll()).thenReturn(List.of());

        // When
        List<PermissionMappingResponseDto> result = service.getAllMappings();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when role has no mappings")
    void shouldReturnEmptyListWhenRoleHasNoMappings() {
        // Given
        String role = "nonexistent";
        when(repository.findByKeycloakRole(role)).thenReturn(List.of());

        // When
        List<PermissionMappingResponseDto> result = service.getMappingsByRole(role);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception for invalid permission value")
    void shouldThrowExceptionForInvalidPermissionValue() {
        // Given
        CreatePermissionMappingDto request = new CreatePermissionMappingDto(
                "admin",
                "invalid:permission"
        );

        // When & Then
        assertThatThrownBy(() -> service.createMapping(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown permission");
    }

    @Test
    @DisplayName("Should create multiple mappings for the same role")
    void shouldCreateMultipleMappingsForTheSameRole() {
        // Given
        String role = "admin";
        CreatePermissionMappingDto request1 = new CreatePermissionMappingDto(
                role,
                "users:read"
        );
        CreatePermissionMappingDto request2 = new CreatePermissionMappingDto(
                role,
                "users:create"
        );

        when(repository.existsByKeycloakRoleAndPermission(role, Permission.USERS_READ))
                .thenReturn(false);
        when(repository.existsByKeycloakRoleAndPermission(role, Permission.USERS_CREATE))
                .thenReturn(false);

        RolePermissionMapping savedMapping1 = new RolePermissionMapping(
                UUID.randomUUID(), role, Permission.USERS_READ
        );
        RolePermissionMapping savedMapping2 = new RolePermissionMapping(
                UUID.randomUUID(), role, Permission.USERS_CREATE
        );

        when(repository.save(any(RolePermissionMapping.class)))
                .thenReturn(savedMapping1)
                .thenReturn(savedMapping2);

        // When
        PermissionMappingResponseDto result1 = service.createMapping(request1);
        PermissionMappingResponseDto result2 = service.createMapping(request2);

        // Then
        assertThat(result1.keycloakRole()).isEqualTo(role);
        assertThat(result1.permission()).isEqualTo("users:read");
        assertThat(result2.keycloakRole()).isEqualTo(role);
        assertThat(result2.permission()).isEqualTo("users:create");
        verify(repository, times(2)).save(any(RolePermissionMapping.class));
    }

    @Test
    @DisplayName("Should convert mapping to DTO correctly")
    void shouldConvertMappingToDtoCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        String role = "admin";
        Permission permission = Permission.USERS_READ;

        RolePermissionMapping mapping = new RolePermissionMapping(id, role, permission);
        when(repository.findAll()).thenReturn(List.of(mapping));

        // When
        List<PermissionMappingResponseDto> result = service.getAllMappings();

        // Then
        assertThat(result).hasSize(1);
        PermissionMappingResponseDto dto = result.getFirst();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.keycloakRole()).isEqualTo(role);
        assertThat(dto.permission()).isEqualTo(permission.getValue());
    }

    @Test
    @DisplayName("Should get mappings for multiple roles")
    void shouldGetMappingsForMultipleRoles() {
        // Given
        String role1 = "admin";
        String role2 = "user";

        RolePermissionMapping mapping1 = new RolePermissionMapping(
                UUID.randomUUID(), role1, Permission.USERS_READ
        );
        RolePermissionMapping mapping2 = new RolePermissionMapping(
                UUID.randomUUID(), role1, Permission.USERS_CREATE
        );
        RolePermissionMapping mapping3 = new RolePermissionMapping(
                UUID.randomUUID(), role2, Permission.REPORTS_READ
        );

        when(repository.findByKeycloakRole(role1)).thenReturn(List.of(mapping1, mapping2));
        when(repository.findByKeycloakRole(role2)).thenReturn(List.of(mapping3));

        // When
        List<PermissionMappingResponseDto> adminMappings = service.getMappingsByRole(role1);
        List<PermissionMappingResponseDto> userMappings = service.getMappingsByRole(role2);

        // Then
        assertThat(adminMappings)
                .hasSize(2)
                .allMatch(dto -> dto.keycloakRole().equals(role1));
        assertThat(userMappings)
                .hasSize(1)
                .allMatch(dto -> dto.keycloakRole().equals(role2));
    }
}
