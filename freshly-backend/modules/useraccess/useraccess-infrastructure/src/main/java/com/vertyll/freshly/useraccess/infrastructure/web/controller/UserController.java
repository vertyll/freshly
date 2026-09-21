package com.vertyll.freshly.useraccess.infrastructure.web.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PagedResponse;
import com.vertyll.freshly.useraccess.application.port.inbound.command.UserAccessCommandUseCase;
import com.vertyll.freshly.useraccess.application.port.inbound.query.UserAccessQueryUseCase;
import com.vertyll.freshly.useraccess.application.security.UserAccessPermission;
import com.vertyll.freshly.useraccess.infrastructure.web.dto.CreateUserRequestDto;
import com.vertyll.freshly.useraccess.infrastructure.web.dto.UpdateUserRolesRequestDto;
import com.vertyll.freshly.useraccess.infrastructure.web.dto.UserResponseDto;
import com.vertyll.freshly.web.http.ETagUtil;
import com.vertyll.freshly.web.security.RequirePermission;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "20";

    private static final String SUBJECT_CLAIM = "sub";

    private final UserAccessCommandUseCase commands;
    private final UserAccessQueryUseCase queries;

    @PostMapping
    @RequirePermission(UserAccessPermission.Values.USERS_CREATE)
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        UserResponseDto body = UserResponseDto.from(commands.createUser(request.toCommand()));

        return ResponseEntity.created(URI.create("/users/" + body.keycloakUserId()))
            .eTag(ETagUtil.buildWeakETag(body.version()))
            .body(body);
    }

    @GetMapping("/{userId}")
    @RequirePermission(UserAccessPermission.Values.USERS_READ)
    public ResponseEntity<UserResponseDto> getUser(@PathVariable UUID userId) {
        UserResponseDto body = UserResponseDto.from(queries.getUser(userId));

        return ResponseEntity.ok().eTag(ETagUtil.buildWeakETag(body.version())).body(body);
    }

    @GetMapping
    @RequirePermission(UserAccessPermission.Values.USERS_READ)
    public PagedResponse<UserResponseDto> listUsers(
        @RequestParam(defaultValue = DEFAULT_PAGE) int page,
        @RequestParam(defaultValue = DEFAULT_SIZE) int size
    ) {
        return queries.listUsers(new PageRequest(page, size)).map(UserResponseDto::from);
    }

    @PatchMapping("/{userId}/activate")
    @RequirePermission(UserAccessPermission.Values.USERS_ACTIVATE)
    public ResponseEntity<Void> activateUser(
        @PathVariable UUID userId,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) @Nullable String ifMatch
    ) {
        commands.activateUser(userId, ETagUtil.parseVersion(ifMatch));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/deactivate")
    @RequirePermission(UserAccessPermission.Values.USERS_DEACTIVATE)
    public ResponseEntity<Void> deactivateUser(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID userId,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) @Nullable String ifMatch
    ) {
        commands.deactivateUser(userId, callerId(jwt), ETagUtil.parseVersion(ifMatch));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/roles")
    @RequirePermission(UserAccessPermission.Values.USERS_MANAGE_ROLES)
    public ResponseEntity<UserResponseDto> replaceRoles(
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateUserRolesRequestDto request,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) @Nullable String ifMatch
    ) {
        UserResponseDto body = UserResponseDto
            .from(commands.replaceUserRoles(request.toCommand(userId, ETagUtil.parseVersion(ifMatch))));

        return ResponseEntity.ok().eTag(ETagUtil.buildWeakETag(body.version())).body(body);
    }

    private static UUID callerId(Jwt jwt) {
        String subject = jwt.getClaimAsString(SUBJECT_CLAIM);
        if (subject == null) {
            throw new AuthenticationCredentialsNotFoundException("Token carries no subject");
        }
        return UUID.fromString(subject);
    }
}
