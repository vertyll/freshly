package com.vertyll.freshly.useraccess.application.service.query;

import java.util.List;

import com.vertyll.freshly.useraccess.application.port.inbound.query.RoleDirectoryQueryUseCase;
import com.vertyll.freshly.useraccess.application.port.outbound.RoleDirectoryPort;

public class RoleDirectoryQueryService implements RoleDirectoryQueryUseCase {
    private final RoleDirectoryPort directory;

    public RoleDirectoryQueryService(RoleDirectoryPort directory) {
        this.directory = directory;
    }

    @Override
    public List<String> availableRoles() {
        return directory.availableRoles().stream().sorted().toList();
    }
}
