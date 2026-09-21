package com.vertyll.freshly.lang.error;

import java.io.Serializable;

/**
 * One entry in a bounded context's error catalogue.
 *
 * <p>
 * Each module declares its own enum implementing this interface. The keys are
 * that module's own, and only it knows what they mean. Implementing a shared
 * interface is what lets a single exception type and a single exception handler
 * serve every module without knowing any of their catalogues.
 */
public interface DomainError extends Serializable {

    /** Translation key naming the failure, resolved against the i18n bundles. */
    String key();

    /** Which HTTP status the failure becomes, decided in one place. */
    ErrorKind kind();
}
