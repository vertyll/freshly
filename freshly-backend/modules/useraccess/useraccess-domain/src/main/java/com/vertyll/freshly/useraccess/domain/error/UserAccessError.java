package com.vertyll.freshly.useraccess.domain.error;

import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.ErrorKind;

public enum UserAccessError implements DomainError {
    USER_NOT_FOUND("error.user.notFound", ErrorKind.NOT_FOUND),
    USER_ALREADY_EXISTS("error.user.alreadyExists", ErrorKind.CONFLICT),
    USER_ALREADY_ACTIVE("error.user.alreadyActive", ErrorKind.CONFLICT),
    USER_ALREADY_INACTIVE("error.user.alreadyInactive", ErrorKind.CONFLICT),
    USER_ROLES_EMPTY("error.user.rolesEmpty", ErrorKind.INVALID),
    UNKNOWN_ROLES("error.user.unknownRoles", ErrorKind.INVALID),

    SELF_DEACTIVATION("error.user.selfDeactivation", ErrorKind.CONFLICT),

    VERSION_MISMATCH("error.common.versionMismatch", ErrorKind.PRECONDITION_FAILED);

    private final String key;
    private final ErrorKind kind;

    UserAccessError(String key, ErrorKind kind) {
        this.key = key;
        this.kind = kind;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public ErrorKind kind() {
        return kind;
    }
}
