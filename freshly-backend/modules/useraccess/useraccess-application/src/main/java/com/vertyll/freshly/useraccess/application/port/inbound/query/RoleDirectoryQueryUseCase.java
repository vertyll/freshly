package com.vertyll.freshly.useraccess.application.port.inbound.query;

import java.util.List;

public interface RoleDirectoryQueryUseCase {
    List<String> availableRoles();
}
