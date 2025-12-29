package com.vertyll.freshly.permission.api;

import com.vertyll.freshly.common.response.ApiResponse;
import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.api.dto.CreatePermissionMappingDto;
import com.vertyll.freshly.permission.api.dto.PermissionMappingResponseDto;
import com.vertyll.freshly.permission.application.PermissionManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PermissionManagementController {

    private final PermissionManagementService permissionManagementService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<String>>> getAvailablePermissions() {
        log.info("Fetching all available permissions");

        List<String> permissions = Arrays.stream(Permission.values())
                .map(Permission::getValue)
                .toList();

        return ApiResponse.buildResponse(
                permissions,
                "Available permissions retrieved successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/mappings")
    public ResponseEntity<ApiResponse<List<PermissionMappingResponseDto>>> getAllMappings() {
        log.info("Fetching all role-permission mappings");

        List<PermissionMappingResponseDto> mappings = permissionManagementService.getAllMappings();

        return ApiResponse.buildResponse(
                mappings,
                "Mappings retrieved successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/mappings/role/{role}")
    public ResponseEntity<ApiResponse<List<PermissionMappingResponseDto>>> getMappingsByRole(
            @PathVariable String role
    ) {

        log.info("Fetching permissions for role: {}", role);

        List<PermissionMappingResponseDto> mappings = permissionManagementService.getMappingsByRole(role);

        return ApiResponse.buildResponse(
                mappings,
                "Role permissions retrieved successfully",
                HttpStatus.OK
        );
    }

    @PostMapping("/mappings")
    public ResponseEntity<ApiResponse<PermissionMappingResponseDto>> createMapping(
            @Valid @RequestBody CreatePermissionMappingDto request
    ) {

        log.info(
                "Creating permission mapping: {} -> {}",
                request.keycloakRole(),
                request.permission()
        );

        PermissionMappingResponseDto mapping = permissionManagementService.createMapping(request);

        return ApiResponse.buildResponse(
                mapping,
                "Permission mapping created successfully",
                HttpStatus.CREATED
        );
    }

    @DeleteMapping("/mappings/{mappingId}")
    public ResponseEntity<ApiResponse<Void>> deleteMapping(@PathVariable UUID mappingId) {
        log.info("Deleting permission mapping: {}", mappingId);

        permissionManagementService.deleteMapping(mappingId);

        return ApiResponse.buildResponse(
                null,
                "Permission mapping deleted successfully",
                HttpStatus.OK
        );
    }
}
