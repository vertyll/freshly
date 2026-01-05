package com.vertyll.freshly.useraccess.infrastructure.persistence.mongo;

import com.vertyll.freshly.useraccess.domain.SystemUser;
import org.springframework.stereotype.Component;

@Component
class SystemUserMapper {

    public SystemUserDocument toDocument(SystemUser user) {
        return new SystemUserDocument(
                user.getKeycloakUserId(),
                user.isActive(),
                user.getRoles()
        );
    }

    public SystemUser toDomain(SystemUserDocument document) {
        return SystemUser.reconstitute(
                document.getKeycloakUserId(),
                document.isActive(),
                document.getRoles()
        );
    }
}
