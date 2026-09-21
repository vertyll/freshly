package com.vertyll.freshly.permission.domain.error;

import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.ErrorKind;

public enum PermissionError implements DomainError {
    ROLE_NOT_FOUND("error.permission.roleNotFound", ErrorKind.NOT_FOUND),

    UNKNOWN_PERMISSION("error.permission.unknownPermission", ErrorKind.INVALID),

    BLANK_ROLE("error.permission.blankRole", ErrorKind.INVALID),

    VERSION_MISMATCH("error.common.versionMismatch", ErrorKind.PRECONDITION_FAILED);

    private final String key;
    private final ErrorKind kind;

    PermissionError(String key, ErrorKind kind) {
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
