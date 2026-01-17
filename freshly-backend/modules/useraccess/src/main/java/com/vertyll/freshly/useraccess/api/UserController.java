package com.vertyll.freshly.useraccess.api;

import java.util.List;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.common.annotation.RequirePermission;
import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.common.response.ApiResponse;
import com.vertyll.freshly.useraccess.api.dto.CreateUserRequestDto;
import com.vertyll.freshly.useraccess.api.dto.UpdateUserRolesRequestDto;
import com.vertyll.freshly.useraccess.api.dto.UserResponseDto;
import com.vertyll.freshly.useraccess.api.mapper.UserDtoMapper;
import com.vertyll.freshly.useraccess.application.UserAccessService;
import com.vertyll.freshly.useraccess.domain.SystemUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private static final String USER_CREATED_MSG_KEY = "success.user.created";
    private static final String USER_FETCHED_MSG_KEY = "success.user.fetched";
    private static final String USER_LIST_FETCHED_MSG_KEY = "success.user.listFetched";
    private static final String USER_ACTIVATED_MSG_KEY = "success.user.activated";
    private static final String USER_DEACTIVATED_MSG_KEY = "success.user.deactivated";
    private static final String USER_ROLES_UPDATED_MSG_KEY = "success.user.rolesUpdated";

    private final UserAccessService userAccessService;
    private final UserDtoMapper userDtoMapper;
    private final MessageSource messageSource;

    @PostMapping
    @RequirePermission(Permission.USERS_CREATE)
    public ResponseEntity<ApiResponse<UserResponseDto>> createUser(
            @Valid @RequestBody CreateUserRequestDto request) {

        log.info("Creating user with keycloak id: {}", request.keycloakUserId());

        SystemUser user =
                userAccessService.createUser(
                        request.keycloakUserId(), request.isActive(), request.roles());

        return ApiResponse.buildResponse(
                userDtoMapper.toResponse(user),
                USER_CREATED_MSG_KEY,
                messageSource,
                HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    @RequirePermission(Permission.USERS_READ)
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable UUID userId) {
        log.info("Fetching user: {}", userId);

        SystemUser user = userAccessService.getUserById(userId);

        return ApiResponse.buildResponse(
                userDtoMapper.toResponse(user), USER_FETCHED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @GetMapping
    @RequirePermission(Permission.USERS_READ)
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        log.info("Fetching all users");

        List<SystemUser> users = userAccessService.getAllUsers();

        return ApiResponse.buildResponse(
                userDtoMapper.toResponseList(users),
                USER_LIST_FETCHED_MSG_KEY,
                messageSource,
                HttpStatus.OK);
    }

    @PatchMapping("/{userId}/activate")
    @RequirePermission(Permission.USERS_ACTIVATE)
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable UUID userId) {
        log.info("Activating user: {}", userId);

        userAccessService.activateUser(userId);

        return ApiResponse.buildResponse(
                null, USER_ACTIVATED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @PatchMapping("/{userId}/deactivate")
    @RequirePermission(Permission.USERS_DEACTIVATE)
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {

        log.info("Deactivating user: {}", userId);

        UUID loggedInUserId = UUID.fromString(jwt.getSubject());
        userAccessService.deactivateUser(userId, loggedInUserId);

        return ApiResponse.buildResponse(
                null, USER_DEACTIVATED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    @PutMapping("/{userId}/roles")
    @RequirePermission(Permission.USERS_MANAGE_ROLES)
    public ResponseEntity<ApiResponse<Void>> updateUserRoles(
            @PathVariable UUID userId, @Valid @RequestBody UpdateUserRolesRequestDto request) {

        log.info("Updating roles for user: {}", userId);

        userAccessService.replaceUserRoles(userId, request.roles());

        return ApiResponse.buildResponse(
                null, USER_ROLES_UPDATED_MSG_KEY, messageSource, HttpStatus.OK);
    }
}
