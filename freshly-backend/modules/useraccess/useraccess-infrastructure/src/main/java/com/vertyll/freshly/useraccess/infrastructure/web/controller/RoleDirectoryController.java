package com.vertyll.freshly.useraccess.infrastructure.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vertyll.freshly.useraccess.application.port.inbound.query.RoleDirectoryQueryUseCase;
import com.vertyll.freshly.useraccess.application.security.UserAccessPermission;
import com.vertyll.freshly.web.security.RequirePermission;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleDirectoryController {
    private final RoleDirectoryQueryUseCase roles;

    @GetMapping
    @RequirePermission(UserAccessPermission.Values.USERS_MANAGE_ROLES)
    public List<String> availableRoles() {
        return roles.availableRoles();
    }
}
