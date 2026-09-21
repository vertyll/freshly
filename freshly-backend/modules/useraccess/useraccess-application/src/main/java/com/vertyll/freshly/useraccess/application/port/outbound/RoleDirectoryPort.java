package com.vertyll.freshly.useraccess.application.port.outbound;

import java.util.Set;
import java.util.UUID;

public interface RoleDirectoryPort {

    Set<String> availableRoles();

    void replaceRoles(UUID keycloakUserId, Set<String> roles);
}
