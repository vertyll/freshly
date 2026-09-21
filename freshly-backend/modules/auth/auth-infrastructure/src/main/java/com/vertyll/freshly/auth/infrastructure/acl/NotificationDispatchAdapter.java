package com.vertyll.freshly.auth.infrastructure.acl;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.auth.application.port.outbound.UserNotificationPort;
import com.vertyll.freshly.notification.application.port.inbound.NotificationCommandUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchAdapter implements UserNotificationPort {
    private final NotificationCommandUseCase notifications;

    @Override
    public void sendEmailVerification(String email, String username, String verificationLink, String languageTag) {
        notifications.sendEmailVerification(email, username, verificationLink, languageTag);
    }

    @Override
    public void sendPasswordReset(String email, String username, String resetLink, String languageTag) {
        notifications.sendPasswordReset(email, username, resetLink, languageTag);
    }

    @Async
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void sendWelcomeEmail(String email, String username, String languageTag) {
        try {
            notifications.sendWelcomeEmail(email, username, languageTag);
        } catch (RuntimeException e) {
            log.error("Welcome email failed for {}; the registration stands", username, e);
        }
    }
}
