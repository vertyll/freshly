package com.vertyll.freshly.translation.domain.error;

import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.ErrorKind;

public enum TranslationError implements DomainError {
    KEY_NOT_FOUND("error.translation.keyNotFound", ErrorKind.NOT_FOUND),
    OVERRIDE_NOT_FOUND("error.translation.overrideNotFound", ErrorKind.NOT_FOUND),
    INVALID_KEY_FORMAT("error.translation.invalidKeyFormat", ErrorKind.INVALID),
    BLANK_OVERRIDE("error.translation.blankOverride", ErrorKind.INVALID),
    UNSUPPORTED_LANGUAGE("error.translation.unsupportedLanguage", ErrorKind.INVALID),
    INVALID_PATTERN("error.translation.invalidPattern", ErrorKind.INVALID),
    PLACEHOLDER_MISMATCH("error.translation.placeholderMismatch", ErrorKind.INVALID),
    IMPORT_UNREADABLE("error.translation.importUnreadable", ErrorKind.INVALID),
    IMPORT_TOO_LARGE("error.translation.importTooLarge", ErrorKind.INVALID),
    UNATTRIBUTABLE_AUTHOR("error.translation.unattributableAuthor", ErrorKind.INVALID),

    KEY_OWNED_BY_ANOTHER_CONTEXT("error.translation.keyOwnedByAnotherContext", ErrorKind.MISCONFIGURED),

    VERSION_MISMATCH("error.common.versionMismatch", ErrorKind.PRECONDITION_FAILED);

    private final String key;
    private final ErrorKind kind;

    TranslationError(String key, ErrorKind kind) {
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
