package com.vertyll.freshly.permission.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.api.dto.CreatePermissionMappingDto;
import com.vertyll.freshly.permission.api.dto.PermissionMappingResponseDto;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;

@ExtendWith(MockitoExtension.class)
class PermissionManagementServiceTest {

    @Mock
    @SuppressWarnings("NullAway.Init")
    private RolePermissionMappingRepository repository;

    @Captor
    @SuppressWarnings("NullAway.Init")
    private ArgumentCaptor<RolePermissionMapping> mappingCaptor;

    private PermissionManagementService service;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        service = new PermissionManagementService(repository);
    }

    @Test
    @DisplayName("Should get all mappings")
    void shouldGetAllMappings() {
        // Given
        RolePermissionMapping mapping1 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        RolePermissionMapping mapping2 =
                new RolePermissionMapping(UserRoleEnum.USER, Permission.REPORTS_READ);
        when(repository.findAll()).thenReturn(List.of(mapping1, mapping2));

        // When
        List<PermissionMappingResponseDto> result = service.getAllMappings();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).keycloakRole()).isEqualTo(UserRoleEnum.ADMIN);
        assertThat(result.get(0).permission()).isEqualTo(Permission.USERS_READ);
        assertThat(result.get(1).keycloakRole()).isEqualTo(UserRoleEnum.USER);
        assertThat(result.get(1).permission()).isEqualTo(Permission.REPORTS_READ);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Should get mappings by role")
    void shouldGetMappingsByRole() {
        // Given
        RolePermissionMapping mapping1 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        RolePermissionMapping mapping2 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_CREATE);
        when(repository.findByKeycloakRole(UserRoleEnum.ADMIN))
                .thenReturn(List.of(mapping1, mapping2));

        // When
        List<PermissionMappingResponseDto> result = service.getMappingsByRole(UserRoleEnum.ADMIN);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).keycloakRole()).isEqualTo(UserRoleEnum.ADMIN);
        assertThat(result.get(0).permission()).isEqualTo(Permission.USERS_READ);
        assertThat(result.get(1).keycloakRole()).isEqualTo(UserRoleEnum.ADMIN);
        assertThat(result.get(1).permission()).isEqualTo(Permission.USERS_CREATE);
        verify(repository).findByKeycloakRole(UserRoleEnum.ADMIN);
    }

    @Test
    @DisplayName("Should create new permission mapping")
    void shouldCreateNewPermissionMapping() {
        // Given
        CreatePermissionMappingDto request =
                new CreatePermissionMappingDto(
                        UserRoleEnum.ADMIN.name(), Permission.USERS_CREATE.getValue());

        when(repository.existsByKeycloakRoleAndPermission(
                        UserRoleEnum.ADMIN, Permission.USERS_CREATE))
                .thenReturn(false);

        RolePermissionMapping savedMapping =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_CREATE);
        when(repository.save(any(RolePermissionMapping.class))).thenReturn(savedMapping);

        // When
        PermissionMappingResponseDto result = service.createMapping(request);

        // Then
        assertThat(result.keycloakRole()).isEqualTo(UserRoleEnum.ADMIN);
        assertThat(result.permission()).isEqualTo(Permission.USERS_CREATE);
        assertThat(result.id()).isEqualTo(savedMapping.getId());

        verify(repository)
                // Corrected the repository method call to convert keycloakRole to UserRoleEnum
                .existsByKeycloakRoleAndPermission(
                        UserRoleEnum.valueOf(request.keycloakRole()), Permission.USERS_CREATE);
        verify(repository).save(mappingCaptor.capture());

        RolePermissionMapping capturedMapping = mappingCaptor.getValue();
        assertThat(capturedMapping.getKeycloakRole()).isEqualTo(UserRoleEnum.ADMIN);
        assertThat(capturedMapping.getPermission()).isEqualTo(Permission.USERS_CREATE);
    }

    @Test
    @DisplayName("Should throw exception when mapping already exists")
    void shouldThrowExceptionWhenMappingAlreadyExists() {
        // Given
        CreatePermissionMappingDto request =
                new CreatePermissionMappingDto(
                        UserRoleEnum.ADMIN.name(), Permission.USERS_CREATE.getValue());

        when(repository.existsByKeycloakRoleAndPermission(
                        UserRoleEnum.valueOf(request.keycloakRole()), Permission.USERS_CREATE))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.createMapping(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mapping already exists")
                .hasMessageContaining(UserRoleEnum.ADMIN.name())
                .hasMessageContaining(Permission.USERS_CREATE.getValue());

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
        UserRoleEnum role = UserRoleEnum.USER;
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
        CreatePermissionMappingDto request =
                new CreatePermissionMappingDto(UserRoleEnum.ADMIN.name(), "INVALID_PERMISSION");

        // When & Then
        assertThatThrownBy(() -> service.createMapping(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown permission");
    }

    @Test
    @DisplayName("Should create multiple mappings for the same role")
    void shouldCreateMultipleMappingsForTheSameRole() {
        // Given
        CreatePermissionMappingDto request1 =
                new CreatePermissionMappingDto(
                        UserRoleEnum.ADMIN.name(), Permission.USERS_READ.getValue());
        CreatePermissionMappingDto request2 =
                new CreatePermissionMappingDto(
                        UserRoleEnum.ADMIN.name(), Permission.USERS_CREATE.getValue());

        when(repository.existsByKeycloakRoleAndPermission(
                        UserRoleEnum.ADMIN, Permission.USERS_READ))
                .thenReturn(false);
        when(repository.existsByKeycloakRoleAndPermission(
                        UserRoleEnum.ADMIN, Permission.USERS_CREATE))
                .thenReturn(false);

        RolePermissionMapping savedMapping1 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        RolePermissionMapping savedMapping2 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_CREATE);

        when(repository.save(any(RolePermissionMapping.class)))
                .thenReturn(savedMapping1)
                .thenReturn(savedMapping2);

        // When
        PermissionMappingResponseDto result1 = service.createMapping(request1);
        PermissionMappingResponseDto result2 = service.createMapping(request2);

        // Then
        assertThat(result1.keycloakRole()).isEqualTo(UserRoleEnum.ADMIN);
        assertThat(result1.permission()).isEqualTo(Permission.USERS_READ);
        assertThat(result2.keycloakRole()).isEqualTo(UserRoleEnum.ADMIN);
        assertThat(result2.permission()).isEqualTo(Permission.USERS_CREATE);
        verify(repository, times(2)).save(any(RolePermissionMapping.class));
    }

    @Test
    @DisplayName("Should convert mapping to DTO correctly")
    void shouldConvertMappingToDtoCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        Permission permission = Permission.USERS_READ;

        RolePermissionMapping mapping =
                RolePermissionMapping.reconstitute(id, UserRoleEnum.ADMIN, permission, null);
        when(repository.findAll()).thenReturn(List.of(mapping));

        // When
        List<PermissionMappingResponseDto> result = service.getAllMappings();

        // Then
        assertThat(result).hasSize(1);
        PermissionMappingResponseDto dto = result.stream().findFirst().orElseThrow();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.keycloakRole()).isEqualTo(UserRoleEnum.ADMIN);
        assertThat(dto.permission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should get mappings for multiple roles")
    void shouldGetMappingsForMultipleRoles() {
        // Given
        RolePermissionMapping mapping1 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        RolePermissionMapping mapping2 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_CREATE);
        RolePermissionMapping mapping3 =
                new RolePermissionMapping(UserRoleEnum.USER, Permission.REPORTS_READ);

        when(repository.findByKeycloakRole(UserRoleEnum.ADMIN))
                .thenReturn(List.of(mapping1, mapping2));
        when(repository.findByKeycloakRole(UserRoleEnum.USER)).thenReturn(List.of(mapping3));

        // When
        List<PermissionMappingResponseDto> adminMappings =
                service.getMappingsByRole(UserRoleEnum.ADMIN);
        List<PermissionMappingResponseDto> userMappings =
                service.getMappingsByRole(UserRoleEnum.USER);

        // Then
        assertThat(adminMappings)
                .hasSize(2)
                .allMatch(dto -> dto.keycloakRole() == UserRoleEnum.ADMIN);
        assertThat(userMappings)
                .hasSize(1)
                .allMatch(dto -> dto.keycloakRole() == UserRoleEnum.USER);
    }
}
