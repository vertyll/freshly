package com.vertyll.freshly.auth.application.port.outbound;

public interface UserNotificationPort {
    void sendEmailVerification(String email, String username, String verificationLink, String languageTag);

    void sendPasswordReset(String email, String username, String resetLink, String languageTag);

    void sendWelcomeEmail(String email, String username, String languageTag);
}
