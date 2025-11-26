package com.vertyll.freshly.useraccess.api;

import com.vertyll.freshly.common.response.ApiResponse;
import com.vertyll.freshly.useraccess.api.dto.CreateUserRequestDto;
import com.vertyll.freshly.useraccess.api.dto.UpdateUserRolesRequestDto;
import com.vertyll.freshly.useraccess.api.dto.UserResponseDto;
import com.vertyll.freshly.useraccess.api.mapper.UserDtoMapper;
import com.vertyll.freshly.useraccess.application.UserAccessService;
import com.vertyll.freshly.useraccess.domain.SystemUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserAccessService userAccessService;
    private final UserDtoMapper userDtoMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> createUser(
            @Valid @RequestBody CreateUserRequestDto request) {

        log.info("Creating user with keycloak id: {}", request.keycloakUserId());

        SystemUser user = userAccessService.createUser(
                request.keycloakUserId(),
                request.isActive(),
                request.roles()
        );

        return ApiResponse.buildResponse(
                userDtoMapper.toResponse(user),
                "User created successfully",
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable UUID userId) {
        log.info("Fetching user: {}", userId);

        SystemUser user = userAccessService.getUserById(userId);

        return ApiResponse.buildResponse(
                userDtoMapper.toResponse(user),
                "User retrieved successfully",
                HttpStatus.OK
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        log.info("Fetching all users");

        List<SystemUser> users = userAccessService.getAllUsers();

        return ApiResponse.buildResponse(
                userDtoMapper.toResponseList(users),
                "Users retrieved successfully",
                HttpStatus.OK
        );
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable UUID userId) {
        log.info("Activating user: {}", userId);

        userAccessService.activateUser(userId);

        return ApiResponse.buildResponse(
                null,
                "User activated successfully",
                HttpStatus.OK
        );
    }

    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Deactivating user: {}", userId);

        UUID loggedInUserId = UUID.fromString(jwt.getSubject());
        userAccessService.deactivateUser(userId, loggedInUserId);

        return ApiResponse.buildResponse(
                null,
                "User deactivated successfully",
                HttpStatus.OK
        );
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<Void>> updateUserRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRolesRequestDto request) {

        log.info("Updating roles for user: {}", userId);

        userAccessService.replaceUserRoles(userId, request.roles());

        return ApiResponse.buildResponse(
                null,
                "User roles updated successfully",
                HttpStatus.OK
        );
    }
}
