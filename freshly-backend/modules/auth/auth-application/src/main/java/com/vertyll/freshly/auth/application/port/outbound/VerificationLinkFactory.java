package com.vertyll.freshly.auth.application.port.outbound;

public interface VerificationLinkFactory {
    String emailVerificationLink(String token);

    String passwordResetLink(String token);
}
