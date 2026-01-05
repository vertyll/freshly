package com.vertyll.freshly.auth.application.eventhandler;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.auth.domain.event.UserRegisteredEvent;
import com.vertyll.freshly.notification.application.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventHandler {

    private final NotificationService notificationService;

    @Async
    @EventListener
    @SuppressWarnings(
            "PMD.AvoidCatchingGenericException") // Email failure shouldn't break registration
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Handling UserRegisteredEvent for user: {}", event.username());

        try {
            notificationService.sendWelcomeEmail(event.email(), event.username());
        } catch (Exception e) {
            log.error("Failed to send welcome email for user: {}", event.username(), e);
            // Don't rethrow - email failure shouldn't break registration
        }
    }
}
