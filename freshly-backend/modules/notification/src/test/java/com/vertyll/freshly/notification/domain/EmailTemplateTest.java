package com.vertyll.freshly.notification.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateTest {

    @Test
    void shouldReturnCorrectTemplateNameForUserRegistered() {
        // When
        String templateName = EmailTemplate.USER_REGISTERED.getTemplateName();

        // Then
        assertThat(templateName).isEqualTo("email/user-registered");
    }

    @Test
    void shouldReturnCorrectTemplateNameForEmailVerification() {
        // When
        String templateName = EmailTemplate.EMAIL_VERIFICATION.getTemplateName();

        // Then
        assertThat(templateName).isEqualTo("email/email-verification");
    }

    @Test
    void shouldReturnCorrectTemplateNameForPasswordReset() {
        // When
        String templateName = EmailTemplate.PASSWORD_RESET.getTemplateName();

        // Then
        assertThat(templateName).isEqualTo("email/password-reset");
    }

    @Test
    void shouldReturnCorrectSubjectForUserRegistered() {
        // When
        String subject = EmailTemplate.USER_REGISTERED.getSubject();

        // Then
        assertThat(subject).isEqualTo("Welcome to Freshly!");
    }

    @Test
    void shouldReturnCorrectSubjectForEmailVerification() {
        // When
        String subject = EmailTemplate.EMAIL_VERIFICATION.getSubject();

        // Then
        assertThat(subject).isEqualTo("Verify Your Email Address");
    }

    @Test
    void shouldReturnCorrectSubjectForPasswordReset() {
        // When
        String subject = EmailTemplate.PASSWORD_RESET.getSubject();

        // Then
        assertThat(subject).isEqualTo("Reset Your Password");
    }

    @Test
    void shouldHaveThreeTemplates() {
        // When
        EmailTemplate[] templates = EmailTemplate.values();

        // Then
        assertThat(templates)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                    EmailTemplate.USER_REGISTERED,
                    EmailTemplate.EMAIL_VERIFICATION,
                    EmailTemplate.PASSWORD_RESET
                );
    }
}
