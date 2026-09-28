package com.vertyll.freshly.notification.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;
import com.vertyll.freshly.notification.application.command.SendEmailCommand;
import com.vertyll.freshly.notification.application.port.outbound.EmailDispatchPort;
import com.vertyll.freshly.notification.domain.error.NotificationError;
import com.vertyll.freshly.notification.domain.model.DeliveryStatus;
import com.vertyll.freshly.notification.domain.model.EmailNotification;
import com.vertyll.freshly.notification.domain.model.EmailTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationCommandServiceTest {
    private static final String RECIPIENT = "reader@example.com";

    private RecordingDispatch dispatch;
    private NotificationCommandService service;

    @BeforeEach
    void setUp() {
        dispatch = new RecordingDispatch();
        service = new NotificationCommandService(dispatch, new RecordingUseCaseLogger());
    }

    @Test
    @DisplayName("dispatches the notification and marks it sent")
    void dispatchesAndMarksSent() {
        service.send(new SendEmailCommand(RECIPIENT, EmailTemplate.USER_REGISTERED, Map.of("username", "ada"), "pl"));

        assertThat(dispatch.dispatched).hasSize(1);
        assertThat(dispatch.dispatched.getFirst().status()).isEqualTo(DeliveryStatus.SENT);
    }

    @Test
    @DisplayName("refuses an invalid recipient before anything is dispatched")
    void refusesInvalidRecipient() {
        assertThatThrownBy(
            () -> service.send(new SendEmailCommand("not-an-address", EmailTemplate.USER_REGISTERED, Map.of(), "pl"))
        ).extracting(e -> ((DomainException) e).error()).isEqualTo(NotificationError.INVALID_RECIPIENT);

        assertThat(dispatch.dispatched).isEmpty();
    }

    @Test
    @DisplayName("a transport failure is recorded and rethrown, not swallowed")
    void rethrowsTransportFailure() {
        dispatch.failWith(new DomainException(NotificationError.DELIVERY_FAILED));

        assertThatThrownBy(
            () -> service.send(new SendEmailCommand(RECIPIENT, EmailTemplate.EMAIL_VERIFICATION, Map.of(), "pl"))
        ).extracting(e -> ((DomainException) e).error()).isEqualTo(NotificationError.DELIVERY_FAILED);
    }

    @Test
    @DisplayName("the language travels with the notification, not with the thread")
    void carriesLanguage() {
        service.sendWelcomeEmail(RECIPIENT, "ada", "pl");

        assertThat(dispatch.dispatched.getFirst().languageTag()).isEqualTo("pl");
    }

    @Test
    @DisplayName("the named methods supply the variables the templates expect")
    void namedMethodsSupplyVariables() {
        service.sendEmailVerification(RECIPIENT, "ada", "https://example.com/verify?token=x", "pl");

        EmailNotification sent = dispatch.dispatched.getFirst();
        assertThat(sent.template()).isEqualTo(EmailTemplate.EMAIL_VERIFICATION);
        assertThat(sent.templateVariables()).containsOnlyKeys("username", "verificationLink");
    }

    private static final class RecordingDispatch implements EmailDispatchPort {
        private final List<EmailNotification> dispatched = new ArrayList<>();
        @Nullable private RuntimeException failure;

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void dispatch(EmailNotification notification) {
            if (failure != null) {
                throw failure;
            }
            dispatched.add(notification);
        }
    }
}
