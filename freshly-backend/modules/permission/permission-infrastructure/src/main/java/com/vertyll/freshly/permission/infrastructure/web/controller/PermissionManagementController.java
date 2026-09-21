package com.vertyll.freshly.permission.infrastructure.web.controller;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vertyll.freshly.authz.CallerRoles;
import com.vertyll.freshly.permission.application.dto.PermissionModuleResponse;
import com.vertyll.freshly.permission.application.dto.RoleAuthorityResponse;
import com.vertyll.freshly.permission.application.port.inbound.command.RoleAuthorityCommandUseCase;
import com.vertyll.freshly.permission.application.port.inbound.query.PermissionQueryUseCase;
import com.vertyll.freshly.permission.application.security.PermissionAdminPermission;
import com.vertyll.freshly.permission.infrastructure.web.dto.ReplaceRoleAuthorityRequestDto;
import com.vertyll.freshly.web.http.ETagUtil;
import com.vertyll.freshly.web.security.RequirePermission;
import com.vertyll.freshly.web.security.ScopedToCaller;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionManagementController {
    private final PermissionQueryUseCase queries;
    private final RoleAuthorityCommandUseCase commands;

    @GetMapping("/me")
    @ScopedToCaller("returns only the calling user's own permissions")
    public Set<String> myPermissions(CallerRoles caller) {
        return queries.permissionsOf(caller);
    }

    @GetMapping("/declared")
    @RequirePermission(PermissionAdminPermission.Values.PERMISSIONS_READ)
    public List<PermissionModuleResponse> declaredPermissions() {
        return queries.declaredPermissions();
    }

    @GetMapping("/roles")
    @RequirePermission(PermissionAdminPermission.Values.PERMISSIONS_READ)
    public List<RoleAuthorityResponse> roles() {
        return queries.listRoles();
    }

    @GetMapping("/roles/{role}")
    @RequirePermission(PermissionAdminPermission.Values.PERMISSIONS_READ)
    public ResponseEntity<RoleAuthorityResponse> role(@PathVariable String role) {
        RoleAuthorityResponse found = queries.roleAuthority(role);

        return ResponseEntity.ok().eTag(ETagUtil.buildWeakETag(found.version())).body(found);
    }

    @PutMapping("/roles/{role}")
    @RequirePermission(PermissionAdminPermission.Values.PERMISSIONS_MANAGE)
    public ResponseEntity<RoleAuthorityResponse> replaceRole(
        @PathVariable String role,
        @Valid @RequestBody ReplaceRoleAuthorityRequestDto request,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) @Nullable String ifMatch
    ) {
        RoleAuthorityResponse updated = commands.replace(request.toCommand(role, ETagUtil.parseVersion(ifMatch)));

        return ResponseEntity.ok().eTag(ETagUtil.buildWeakETag(updated.version())).body(updated);
    }

    @DeleteMapping("/roles/{role}")
    @RequirePermission(PermissionAdminPermission.Values.PERMISSIONS_MANAGE)
    public ResponseEntity<Void> deleteRole(@PathVariable String role) {
        commands.delete(role);
        return ResponseEntity.noContent().build();
    }
}
