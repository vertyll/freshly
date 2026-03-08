package com.vertyll.freshly.permission.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.common.response.ApiResponse;
import com.vertyll.freshly.permission.api.dto.CreatePermissionMappingDto;
import com.vertyll.freshly.permission.api.dto.PermissionMappingResponseDto;
import com.vertyll.freshly.permission.application.PermissionManagementService;

@ExtendWith(MockitoExtension.class)
class PermissionManagementControllerTest {

    private static final String SUCCESS = "Success";
    private static final String FETCHING_AVAILABLE_PERMISSIONS_MSG_KEY =
            "success.permission.availableFetched";
    private static final String FETCHING_ROLE_PERMISSION_MAPPINGS_MSG_KEY =
            "success.permission.mappingsFetched";
    private static final String FETCHING_PERMISSIONS_FOR_ROLE_MSG_KEY =
            "success.permission.mappingsByRoleFetched";
    private static final String CREATING_PERMISSION_MAPPING_MSG_KEY =
            "success.permission.mappingCreated";
    private static final String DELETING_PERMISSION_MAPPING_MSG_KEY =
            "success.permission.mappingDeleted";

    @Mock
    @SuppressWarnings("NullAway.Init")
    private PermissionManagementService permissionManagementService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MessageSource messageSource;

    private PermissionManagementController permissionManagementController;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        permissionManagementController =
                new PermissionManagementController(permissionManagementService, messageSource);
    }

    @Test
    @DisplayName("Should return all available permissions")
    void shouldReturnAllAvailablePermissions() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);

        // When
        ResponseEntity<ApiResponse<List<String>>> response =
                permissionManagementController.getAvailablePermissions();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotEmpty();
        verify(messageSource)
                .getMessage(eq(FETCHING_AVAILABLE_PERMISSIONS_MSG_KEY), any(), any(Locale.class));
    }

    @Test
    @DisplayName("Should return all Permission enum values as strings")
    void shouldReturnAllPermissionEnumValuesAsStrings() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);

        // When
        ResponseEntity<ApiResponse<List<String>>> response =
                permissionManagementController.getAvailablePermissions();

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData())
                .hasSize(Permission.values().length)
                .containsExactlyInAnyOrderElementsOf(
                        java.util.Arrays.stream(Permission.values())
                                .map(Permission::getValue)
                                .toList());
    }

    @Test
    @DisplayName("Should return all role-permission mappings")
    void shouldReturnAllRolePermissionMappings() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);
        UUID id = UUID.randomUUID();
        List<PermissionMappingResponseDto> mappings =
                List.of(
                        new PermissionMappingResponseDto(
                                id, "ROLE_ADMIN", Permission.USERS_READ, 1L));
        when(permissionManagementService.getAllMappings()).thenReturn(mappings);

        // When
        ResponseEntity<ApiResponse<List<PermissionMappingResponseDto>>> response =
                permissionManagementController.getAllMappings();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().getFirst().id()).isEqualTo(id);
        verify(permissionManagementService).getAllMappings();
        verify(messageSource)
                .getMessage(
                        eq(FETCHING_ROLE_PERMISSION_MAPPINGS_MSG_KEY), any(), any(Locale.class));
    }

    @Test
    @DisplayName("Should return empty list when no mappings exist")
    void shouldReturnEmptyListWhenNoMappingsExist() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);
        when(permissionManagementService.getAllMappings()).thenReturn(List.of());

        // When
        ResponseEntity<ApiResponse<List<PermissionMappingResponseDto>>> response =
                permissionManagementController.getAllMappings();

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEmpty();
    }

    @Test
    @DisplayName("Should return mappings for given role")
    void shouldReturnMappingsForGivenRole() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);
        UUID id = UUID.randomUUID();
        List<PermissionMappingResponseDto> mappings =
                List.of(
                        new PermissionMappingResponseDto(
                                id, "ROLE_USER", Permission.USERS_READ, 1L));
        when(permissionManagementService.getMappingsByRole("ROLE_USER")).thenReturn(mappings);

        // When
        ResponseEntity<ApiResponse<List<PermissionMappingResponseDto>>> response =
                permissionManagementController.getMappingsByRole("ROLE_USER");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().getFirst().keycloakRole()).isEqualTo("ROLE_USER");
        verify(permissionManagementService).getMappingsByRole("ROLE_USER");
        verify(messageSource)
                .getMessage(eq(FETCHING_PERMISSIONS_FOR_ROLE_MSG_KEY), any(), any(Locale.class));
    }

    @Test
    @DisplayName("Should return empty list when no mappings exist for role")
    void shouldReturnEmptyListWhenNoMappingsExistForRole() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);
        when(permissionManagementService.getMappingsByRole("ROLE_UNKNOWN")).thenReturn(List.of());

        // When
        ResponseEntity<ApiResponse<List<PermissionMappingResponseDto>>> response =
                permissionManagementController.getMappingsByRole("ROLE_UNKNOWN");

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEmpty();
    }

    @Test
    @DisplayName("Should create permission mapping and return 201 with ETag")
    void shouldCreatePermissionMappingAndReturn201WithETag() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);
        UUID id = UUID.randomUUID();
        CreatePermissionMappingDto request =
                new CreatePermissionMappingDto("ROLE_USER", Permission.USERS_READ);
        PermissionMappingResponseDto created =
                new PermissionMappingResponseDto(id, "ROLE_USER", Permission.USERS_READ, 1L);
        when(permissionManagementService.createMapping(request)).thenReturn(created);

        // When
        ResponseEntity<ApiResponse<PermissionMappingResponseDto>> response =
                permissionManagementController.createMapping(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ETAG)).isEqualTo("W/\"1\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().id()).isEqualTo(id);
        verify(permissionManagementService).createMapping(request);
        verify(messageSource)
                .getMessage(eq(CREATING_PERMISSION_MAPPING_MSG_KEY), any(), any(Locale.class));
    }

    @Test
    @DisplayName("Should create mapping without ETag when version is null")
    void shouldCreateMappingWithoutETagWhenVersionIsNull() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);
        UUID id = UUID.randomUUID();
        CreatePermissionMappingDto request =
                new CreatePermissionMappingDto("ROLE_USER", Permission.USERS_READ);
        PermissionMappingResponseDto created =
                new PermissionMappingResponseDto(id, "ROLE_USER", Permission.USERS_READ, null);
        when(permissionManagementService.createMapping(request)).thenReturn(created);

        // When
        ResponseEntity<ApiResponse<PermissionMappingResponseDto>> response =
                permissionManagementController.createMapping(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ETAG)).isNull();
    }

    @Test
    @DisplayName("Should delete permission mapping with valid If-Match version")
    void shouldDeletePermissionMappingWithValidIfMatchVersion() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);
        UUID mappingId = UUID.randomUUID();
        doNothing().when(permissionManagementService).deleteMapping(mappingId, 1L);

        // When
        ResponseEntity<ApiResponse<Void>> response =
                permissionManagementController.deleteMapping(mappingId, "W/\"1\"");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(permissionManagementService).deleteMapping(mappingId, 1L);
        verify(messageSource)
                .getMessage(eq(DELETING_PERMISSION_MAPPING_MSG_KEY), any(), any(Locale.class));
    }

    @Test
    @DisplayName("Should delete permission mapping without If-Match header")
    void shouldDeletePermissionMappingWithoutIfMatchHeader() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);
        UUID mappingId = UUID.randomUUID();
        doNothing().when(permissionManagementService).deleteMapping(mappingId, null);

        // When
        ResponseEntity<ApiResponse<Void>> response =
                permissionManagementController.deleteMapping(mappingId, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(permissionManagementService).deleteMapping(mappingId, null);
    }

    @Test
    @DisplayName("Should pass null version to service when If-Match header is unparseable")
    void shouldPassNullVersionToServiceWhenIfMatchHeaderIsUnparseable() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn(SUCCESS);
        UUID mappingId = UUID.randomUUID();
        doNothing().when(permissionManagementService).deleteMapping(mappingId, null);

        // When
        ResponseEntity<ApiResponse<Void>> response =
                permissionManagementController.deleteMapping(mappingId, "not-a-version");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(permissionManagementService).deleteMapping(mappingId, null);
    }
}
