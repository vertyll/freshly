package com.vertyll.freshly.web.http;

import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.ErrorKind;

/**
 * Refusals raised while reading an {@code If-Match} header, before any module is involved.
 *
 * <p>
 * Owned by the platform because the header is: every module reads it through
 * {@link ETagUtil}, and a malformed one is the same mistake whichever endpoint it reaches.
 */
public enum IfMatchError implements DomainError {
    MALFORMED("error.common.malformedIfMatch", ErrorKind.INVALID);

    private final String key;
    private final ErrorKind kind;

    IfMatchError(String key, ErrorKind kind) {
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
