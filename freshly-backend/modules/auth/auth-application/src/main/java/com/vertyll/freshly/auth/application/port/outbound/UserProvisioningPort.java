package com.vertyll.freshly.auth.application.port.outbound;

import java.util.Set;
import java.util.UUID;

public interface UserProvisioningPort {
    void provision(UUID userId, Set<String> roles, boolean active);

    void activate(UUID userId);

    void deactivate(UUID userId);

    boolean deprovision(UUID userId);
}
